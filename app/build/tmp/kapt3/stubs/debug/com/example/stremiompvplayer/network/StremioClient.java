package com.example.stremiompvplayer.network;

import com.example.stremiompvplayer.models.CatalogResponse;
import com.example.stremiompvplayer.models.FeedList;
import com.example.stremiompvplayer.models.Manifest;
import com.example.stremiompvplayer.models.MetaResponse;
import com.example.stremiompvplayer.models.StreamResponse;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\u000eB\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0016\u0010\t\u001a\n \u000b*\u0004\u0018\u00010\n0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\f\u001a\n \u000b*\u0004\u0018\u00010\r0\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/example/stremiompvplayer/network/StremioClient;", "", "()V", "BASE_URL", "", "api", "Lcom/example/stremiompvplayer/network/StremioClient$StremioApi;", "getApi", "()Lcom/example/stremiompvplayer/network/StremioClient$StremioApi;", "moshi", "Lcom/squareup/moshi/Moshi;", "kotlin.jvm.PlatformType", "retrofit", "Lretrofit2/Retrofit;", "StremioApi", "app_debug"})
public final class StremioClient {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String BASE_URL = "https://api.strem.io";
    private static final com.squareup.moshi.Moshi moshi = null;
    private static final retrofit2.Retrofit retrofit = null;
    @org.jetbrains.annotations.NotNull()
    private static final com.example.stremiompvplayer.network.StremioClient.StremioApi api = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.stremiompvplayer.network.StremioClient INSTANCE = null;
    
    private StremioClient() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.stremiompvplayer.network.StremioClient.StremioApi getApi() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u001e\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\b\b\u0001\u0010\n\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u000b\u001a\u00020\f2\b\b\u0001\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J,\u0010\r\u001a\u00020\u000e2\b\b\u0001\u0010\n\u001a\u00020\u00052\b\b\u0001\u0010\u000f\u001a\u00020\u00052\b\b\u0001\u0010\u0010\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0011J,\u0010\u0012\u001a\u00020\u00132\b\b\u0001\u0010\n\u001a\u00020\u00052\b\b\u0001\u0010\u000f\u001a\u00020\u00052\b\b\u0001\u0010\u0010\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0011J(\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\t0\b2\b\b\u0001\u0010\n\u001a\u00020\u00052\b\b\u0001\u0010\u0015\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0016\u00a8\u0006\u0017"}, d2 = {"Lcom/example/stremiompvplayer/network/StremioClient$StremioApi;", "", "getCatalog", "Lcom/example/stremiompvplayer/models/CatalogResponse;", "url", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getFeed", "", "Lcom/example/stremiompvplayer/models/FeedList;", "authKey", "getManifest", "Lcom/example/stremiompvplayer/models/Manifest;", "getMeta", "Lcom/example/stremiompvplayer/models/MetaResponse;", "type", "id", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getStreams", "Lcom/example/stremiompvplayer/models/StreamResponse;", "search", "query", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
    public static abstract interface StremioApi {
        
        @retrofit2.http.GET(value = "/api/feed?authKey={authKey}")
        @org.jetbrains.annotations.Nullable()
        public abstract java.lang.Object getFeed(@retrofit2.http.Path(value = "authKey")
        @org.jetbrains.annotations.NotNull()
        java.lang.String authKey, @org.jetbrains.annotations.NotNull()
        kotlin.coroutines.Continuation<? super java.util.List<com.example.stremiompvplayer.models.FeedList>> $completion);
        
        @retrofit2.http.GET(value = "/api/meta?authKey={authKey}&type={type}&id={id}")
        @org.jetbrains.annotations.Nullable()
        public abstract java.lang.Object getMeta(@retrofit2.http.Path(value = "authKey")
        @org.jetbrains.annotations.NotNull()
        java.lang.String authKey, @retrofit2.http.Path(value = "type")
        @org.jetbrains.annotations.NotNull()
        java.lang.String type, @retrofit2.http.Path(value = "id")
        @org.jetbrains.annotations.NotNull()
        java.lang.String id, @org.jetbrains.annotations.NotNull()
        kotlin.coroutines.Continuation<? super com.example.stremiompvplayer.models.MetaResponse> $completion);
        
        @retrofit2.http.GET(value = "/api/streams?authKey={authKey}&type={type}&id={id}")
        @org.jetbrains.annotations.Nullable()
        public abstract java.lang.Object getStreams(@retrofit2.http.Path(value = "authKey")
        @org.jetbrains.annotations.NotNull()
        java.lang.String authKey, @retrofit2.http.Path(value = "type")
        @org.jetbrains.annotations.NotNull()
        java.lang.String type, @retrofit2.http.Path(value = "id")
        @org.jetbrains.annotations.NotNull()
        java.lang.String id, @org.jetbrains.annotations.NotNull()
        kotlin.coroutines.Continuation<? super com.example.stremiompvplayer.models.StreamResponse> $completion);
        
        @retrofit2.http.GET(value = "/api/search?authKey={authKey}&query={query}")
        @org.jetbrains.annotations.Nullable()
        public abstract java.lang.Object search(@retrofit2.http.Path(value = "authKey")
        @org.jetbrains.annotations.NotNull()
        java.lang.String authKey, @retrofit2.http.Path(value = "query")
        @org.jetbrains.annotations.NotNull()
        java.lang.String query, @org.jetbrains.annotations.NotNull()
        kotlin.coroutines.Continuation<? super java.util.List<com.example.stremiompvplayer.models.FeedList>> $completion);
        
        /**
         * Fetches the manifest.json from a specific add-on.
         * @param url The full URL to the add-on's manifest (e.g., "http://127.0.0.1:7878/manifest.json")
         */
        @retrofit2.http.GET()
        @org.jetbrains.annotations.Nullable()
        public abstract java.lang.Object getManifest(@retrofit2.http.Url()
        @org.jetbrains.annotations.NotNull()
        java.lang.String url, @org.jetbrains.annotations.NotNull()
        kotlin.coroutines.Continuation<? super com.example.stremiompvplayer.models.Manifest> $completion);
        
        /**
         * Fetches a specific catalog from an add-on.
         * @param url The full URL to the catalog (e.g., "http://127.0.0.1:7878/catalog/movie/top.json")
         */
        @retrofit2.http.GET()
        @org.jetbrains.annotations.Nullable()
        public abstract java.lang.Object getCatalog(@retrofit2.http.Url()
        @org.jetbrains.annotations.NotNull()
        java.lang.String url, @org.jetbrains.annotations.NotNull()
        kotlin.coroutines.Continuation<? super com.example.stremiompvplayer.models.CatalogResponse> $completion);
    }
}