/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.di

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.ochenjoshua.ojmusicplayer.constants.MaxSongCacheSizeKey
import com.ochenjoshua.ojmusicplayer.data.repository.UpdateRepositoryImpl
import com.ochenjoshua.ojmusicplayer.db.InternalDatabase
import com.ochenjoshua.ojmusicplayer.db.MusicDatabase
import com.ochenjoshua.ojmusicplayer.domain.repository.UpdateRepository
import com.ochenjoshua.ojmusicplayer.storage.StorageFolderKind
import com.ochenjoshua.ojmusicplayer.storage.StorageLocationRepository
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.get
import java.io.File
import java.util.NavigableSet
import java.util.TreeSet
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache

private class LazyCache(
    private val create: () -> SimpleCache,
) : Cache {
    private val lock = Any()

    @Volatile private var cache: SimpleCache? = null

    private fun delegateLocked(): SimpleCache = cache ?: create().also { cache = it }

    private inline fun <T> withDelegate(block: (SimpleCache) -> T): T =
        synchronized(lock) {
            block(delegateLocked())
        }

    override fun addListener(
        key: String,
        listener: Cache.Listener,
    ) = withDelegate { cache -> cache.addListener(key, listener) }

    override fun removeListener(
        key: String,
        listener: Cache.Listener,
    ) = withDelegate { cache -> cache.removeListener(key, listener) }

    override fun getCachedSpans(key: String): NavigableSet<CacheSpan> = withDelegate { cache -> cache.getCachedSpans(key) }

    override fun getKeys(): NavigableSet<String> = withDelegate { cache -> TreeSet(cache.keys) }

    override fun getCacheSpace(): Long = withDelegate { cache -> cache.cacheSpace }

    override fun getUid(): Long = withDelegate { cache -> cache.uid }

    override fun getCachedLength(
        key: String,
        position: Long,
        length: Long,
    ): Long = withDelegate { cache -> cache.getCachedLength(key, position, length) }

    override fun getCachedBytes(
        key: String,
        position: Long,
        length: Long,
    ): Long = withDelegate { cache -> cache.getCachedBytes(key, position, length) }

    override fun applyContentMetadataMutations(
        key: String,
        mutations: ContentMetadataMutations,
    ) = withDelegate { cache -> cache.applyContentMetadataMutations(key, mutations) }

    override fun getContentMetadata(key: String): ContentMetadata = withDelegate { cache -> cache.getContentMetadata(key) }

    override fun startReadWrite(
        key: String,
        position: Long,
        length: Long,
    ): CacheSpan = withDelegate { cache -> cache.startReadWrite(key, position, length) }

    override fun startReadWriteNonBlocking(
        key: String,
        position: Long,
        length: Long,
    ): CacheSpan? = withDelegate { cache -> cache.startReadWriteNonBlocking(key, position, length) }

    override fun startFile(
        key: String,
        position: Long,
        maxLength: Long,
    ): File = withDelegate { cache -> cache.startFile(key, position, maxLength) }

    override fun commitFile(
        file: File,
        length: Long,
    ) = withDelegate { cache -> cache.commitFile(file, length) }

    override fun releaseHoleSpan(holeSpan: CacheSpan) = withDelegate { cache -> cache.releaseHoleSpan(holeSpan) }

    override fun removeSpan(span: CacheSpan) = withDelegate { cache -> cache.removeSpan(span) }

    override fun removeResource(key: String) = withDelegate { cache -> cache.removeResource(key) }

    override fun isCached(
        key: String,
        position: Long,
        length: Long,
    ): Boolean = withDelegate { cache -> cache.isCached(key, position, length) }

    override fun release() {
        synchronized(lock) {
            cache?.release()
            cache = null
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MusicDatabase = InternalDatabase.newInstance(context)

    @Singleton
    @Provides
    fun provideDatabaseProvider(
        @ApplicationContext context: Context,
    ): DatabaseProvider = StandaloneDatabaseProvider(context)

    @Singleton
    @Provides
    @PlayerCache
    fun providePlayerCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): Cache =
        LazyCache {
            val cacheSize = context.dataStore.get(MaxSongCacheSizeKey, 1024)
            val evictor =
                when (cacheSize) {
                    -1 -> NoOpCacheEvictor()
                    else -> LeastRecentlyUsedCacheEvictor(cacheSizeMegabytesToBytes(cacheSize))
                }
            SimpleCache(
                StorageLocationRepository.cacheDirectory(context, StorageFolderKind.SONG_CACHE),
                evictor,
                databaseProvider,
            )
        }

    @Singleton
    @Provides
    @DownloadCache
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): Cache =
        LazyCache {
            SimpleCache(
                StorageLocationRepository.cacheDirectory(context, StorageFolderKind.DOWNLOADS),
                NoOpCacheEvictor(),
                databaseProvider,
            )
        }

    @Singleton
    @Provides
    fun provideUpdateRepository(
        impl: UpdateRepositoryImpl
    ): UpdateRepository = impl

    @Singleton
    @Provides
    fun provideFlacConfig(@ApplicationContext context: Context): com.ochenjoshua.ojmusicplayer.flaccore.FlacConfig {
        return com.ochenjoshua.ojmusicplayer.lossless.FlacConfigImpl(context)
    }

    @Singleton
    @Provides
    fun provideFlacKvStore(@ApplicationContext context: Context): com.ochenjoshua.ojmusicplayer.flaccore.FlacKvStore {
        return com.ochenjoshua.ojmusicplayer.lossless.FlacKvStoreImpl(context)
    }

    @Singleton
    @Provides
    fun provideQbdlxSigner(): com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxSigner {
        return com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxSigner()
    }

    @Singleton
    @Provides
    fun provideQbdlxCredentialStore(
        config: com.ochenjoshua.ojmusicplayer.flaccore.FlacConfig,
        kvStore: com.ochenjoshua.ojmusicplayer.flaccore.FlacKvStore
    ): com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxCredentialStore {
        return com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxCredentialStore(
            config = config,
            kvStore = kvStore,
            poolProvider = com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxPoolProvider { kotlinx.coroutines.runBlocking { config.qbdlxTokenPool() } }
        )
    }

    @Singleton
    @Provides
    fun provideQbdlxApiClient(
        config: com.ochenjoshua.ojmusicplayer.flaccore.FlacConfig,
        signer: com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxSigner,
        credentialStore: com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxCredentialStore
    ): com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxApiClient {
        val sharedClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        return com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxApiClient(
            config = config,
            sharedClient = sharedClient,
            signer = signer,
            signingResolver = credentialStore
        )
    }

    @Singleton
    @Provides
    fun provideAggregatorRateLimiter(): com.ochenjoshua.ojmusicplayer.flaccore.ratelimit.AggregatorRateLimiter {
        return com.ochenjoshua.ojmusicplayer.flaccore.ratelimit.AggregatorRateLimiter()
    }

    @Singleton
    @Provides
    fun provideQbdlxQobuzSource(
        apiClient: com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxApiClient,
        credentialStore: com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxCredentialStore,
        rateLimiter: com.ochenjoshua.ojmusicplayer.flaccore.ratelimit.AggregatorRateLimiter,
        config: com.ochenjoshua.ojmusicplayer.flaccore.FlacConfig
    ): com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxQobuzSource {
        return com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxQobuzSource(
            apiClient = apiClient,
            credentialStore = credentialStore,
            rateLimiter = rateLimiter,
            config = config
        )
    }

    @Singleton
    @Provides
    fun provideQbdlxStreamResolver(source: com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxQobuzSource): com.ochenjoshua.ojmusicplayer.flaccore.streaming.QbdlxStreamResolver {
        return com.ochenjoshua.ojmusicplayer.flaccore.streaming.QbdlxStreamResolver(source)
    }

    @Singleton
    @Provides
    fun provideFlacStreamRegistry(
        qbdlxResolver: com.ochenjoshua.ojmusicplayer.flaccore.streaming.QbdlxStreamResolver,
    ): com.ochenjoshua.ojmusicplayer.flaccore.streaming.FlacStreamRegistry {
        return com.ochenjoshua.ojmusicplayer.flaccore.streaming.FlacStreamRegistry(
            qbdlx = { q, ql -> qbdlxResolver.resolve(q, ql) },
        )
    }

    @Singleton
    @Provides
    fun provideLosslessStreamResolver(
        registry: com.ochenjoshua.ojmusicplayer.flaccore.streaming.FlacStreamRegistry
    ): com.ochenjoshua.ojmusicplayer.playback.resolvers.LosslessStreamResolver {
        return com.ochenjoshua.ojmusicplayer.lossless.FlacCoreLosslessStreamResolver(registry)
    }
}


private const val CacheSizeBytesPerMegabyte = 1024L * 1024L

private fun cacheSizeMegabytesToBytes(sizeMegabytes: Int): Long = sizeMegabytes.toLong().coerceAtLeast(0L) * CacheSizeBytesPerMegabyte
