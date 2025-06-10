import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.api.CacheKey
import com.apollographql.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.cache.normalized.apolloStore
import com.apollographql.cache.normalized.fetchFromCache
import com.apollographql.cache.normalized.fetchPolicy
import com.apollographql.cache.normalized.fetchPolicyInterceptor
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.normalizedCache
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.example.GetBookmarksQuery
import com.example.GetHomeQuery
import com.example.cache.Cache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals

class MainTest {
  // language=json
  val home = """
    {
      "data": {
        "home": {
          "__typename": "Timeline", 
          "entries": [
            {"__typename": "Entry", "id": "0", "content": {"__typename":  "Post", "id": "42", "body": "Lorem ipsum 1"} },  
            {"__typename": "Entry", "id": "2", "content": {"__typename":  "User", "id": "42","avatar": null, "name": "FooBar" } }  
          ] 
        }
      }
    }
  """.trimIndent()

  // language=json
  val bookmarks = """
    {
      "data": {
        "bookmarks": {
          "__typename": "Timeline", 
          "entries": [
            {"__typename": "Entry", "id": "10", "content": {"__typename": "Post", "id": "42", "body": "Lorem ipsum 1"} },  
            {"__typename": "Entry", "id": "11", "content": {"__typename": "Post", "id": "43", "body": "Lorem ipsum 2"} } 
          ] 
        }
      }
    }
  """.trimIndent()


  var debug: Any? = null

  @Test
  fun test() = runBlocking {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
        .normalizedCache(MemoryCacheFactory(), TypePolicyCacheKeyGenerator(Cache.typePolicies))
        .serverUrl(mockServer.url())
        .build()
        .use {apolloClient ->
//          mockServer.enqueueString(home)
//          val home = apolloClient.query(GetHomeQuery()).execute()

          mockServer.enqueueString(bookmarks)
          var bookmarks = apolloClient.query(GetBookmarksQuery()).fetchPolicy(FetchPolicy.NetworkOnly).execute()
          debug = bookmarks
          assertEquals(
            true,
            bookmarks.data?.bookmarks?.timelineDetails?.entries?.any { it?.content?.onPost?.id == "42" },
          )

          debug = apolloClient.apolloStore.dump()
          // Remove the "42" post
          apolloClient.apolloStore.remove(CacheKey("Post", "42"))

          debug = apolloClient.apolloStore.dump()
          bookmarks = apolloClient.query(GetBookmarksQuery()).fetchPolicyInterceptor(PartialCacheOnlyInterceptor).toFlow().single()
          debug = bookmarks

          assertEquals(
            false,
            bookmarks.data?.bookmarks?.timelineDetails?.entries?.any { it?.content?.onPost?.id == "42" },
          )
        }
    }
  }
}

val PartialCacheOnlyInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(
      request = request
        .newBuilder()
        .fetchFromCache(true)
        .build()
    )
  }
}