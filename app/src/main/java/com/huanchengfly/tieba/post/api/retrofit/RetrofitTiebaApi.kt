package com.huanchengfly.tieba.post.api.retrofit

import android.os.Build
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.ClientVersion
import com.huanchengfly.tieba.post.api.Header
import com.huanchengfly.tieba.post.api.Param
import com.huanchengfly.tieba.post.api.getCookie
import com.huanchengfly.tieba.post.api.getUserAgent
import com.huanchengfly.tieba.post.api.models.OAID
import com.huanchengfly.tieba.post.api.retrofit.adapter.DeferredCallAdapterFactory
import com.huanchengfly.tieba.post.api.retrofit.adapter.FlowCallAdapterFactory
import com.huanchengfly.tieba.post.api.retrofit.converter.gson.GsonConverterFactory
import com.huanchengfly.tieba.post.api.retrofit.converter.kotlinx.serialization.asConverterFactory
import com.huanchengfly.tieba.post.api.retrofit.interceptors.AddWebCookieInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.CommonHeaderInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.CommonParamInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.CookieInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.DropInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.FailureResponseInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ForceLoginInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ProtoFailureResponseInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.SortAndSignInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.StParamInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interfaces.AppHybridTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.interfaces.MiniTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.interfaces.NewTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.interfaces.OfficialProtobufTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.interfaces.OfficialTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.interfaces.SofireApi
import com.huanchengfly.tieba.post.api.retrofit.interfaces.WebTiebaApi
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.CacheUtil.base64Encode
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.DeviceUtils
import com.huanchengfly.tieba.post.utils.AppPrivacyManager
import com.huanchengfly.tieba.post.utils.UIDUtil
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


object RetrofitTiebaApi {
    private const val READ_TIMEOUT = 60L
    private const val CONNECT_TIMEOUT = 60L
    private const val WRITE_TIMEOUT = 60L

    private val initTime = System.currentTimeMillis()
    internal val randomClientId = "wappc_${initTime}_${(Math.random() * 1000).roundToInt()}"
    private val stParamInterceptor = StParamInterceptor()
    private val connectionPool = ConnectionPool(32, 5, TimeUnit.MINUTES)

    private val defaultCommonParamInterceptor = CommonParamInterceptor(
        Param.BDUSS to { AccountUtil.getBduss() },
        Param.CLIENT_ID to { AppPrivacyManager.getClientId() },
        Param.CLIENT_TYPE to { "2" },
        Param.OS_VERSION to { AppPrivacyManager.getOsVersion() },
        Param.MODEL to { AppPrivacyManager.getModel() },
        Param.NET_TYPE to { AppPrivacyManager.getNetType() },
        Param.PHONE_IMEI to { AppPrivacyManager.getPhoneImei(App.INSTANCE) },
        Param.TIMESTAMP to { System.currentTimeMillis().toString() }
    )

    private val defaultCommonHeaderInterceptor =
        CommonHeaderInterceptor(
            Header.COOKIE to { "ka=open" },
            Header.PRAGMA to { "no-cache" }
        )
    private val gsonConverterFactory = GsonConverterFactory.create()
    private val sortAndSignInterceptor = SortAndSignInterceptor("tiebaclient!!!")

    val NEW_TIEBA_API: NewTiebaApi by lazy {
        createJsonApi<NewTiebaApi>(
            "http://c.tieba.baidu.com/",
            defaultCommonHeaderInterceptor,
            CommonHeaderInterceptor(
                Header.USER_AGENT to { "bdtb for Android 8.2.2" },
                Header.CUID to { UIDUtil.finalCUID }
            ),
            defaultCommonParamInterceptor + CommonParamInterceptor(
                Param.CUID to { UIDUtil.finalCUID },
                Param.FROM to { "baidu_appstore" },
                Param.CLIENT_VERSION to { "8.2.2" }
            ),
            stParamInterceptor,
        )
    }

    val WEB_TIEBA_API: WebTiebaApi by lazy {
        createJsonApi<WebTiebaApi>("https://tieba.baidu.com/",
            CommonHeaderInterceptor(
                Header.USER_AGENT to { getUserAgent("tieba/11.10.8.6 skin/default") },
                Header.CUID to { AppPrivacyManager.getCuid() },
                Header.CUID_GALAXY2 to { AppPrivacyManager.getCuidGalaxy2() },
                Header.CUID_GID to { "" },
                Header.CUID_GALAXY3 to { AppPrivacyManager.getC3Aid() },
                Header.CLIENT_USER_TOKEN to { AccountUtil.getUid() },
                Header.CHARSET to { "UTF-8" },
                Header.HOST to { "tieba.baidu.com" },
            ),
            AddWebCookieInterceptor
        )
    }

    val HYBRID_TIEBA_API: AppHybridTiebaApi by lazy {
        createJsonApi<AppHybridTiebaApi>("https://tieba.baidu.com/",
            CommonHeaderInterceptor(
                Header.USER_AGENT to { getUserAgent("tieba/12.35.1.0 skin/default") },
                Header.HOST to { "tieba.baidu.com" },
                Header.PRAGMA to { "no-cache" },
                Header.CACHE_CONTROL to { "no-cache" },
                Header.ACCEPT to { "application/json, text/plain, */*" },
                Header.ACCEPT_LANGUAGE to { Header.ACCEPT_LANGUAGE_VALUE },
                "X-Requested-With" to { "com.baidu.tieba" },
                "Sec-Fetch-Site" to { "same-origin" },
                "Sec-Fetch-Mode" to { "cors" },
                "Sec-Fetch-Dest" to { "empty" },
                Header.COOKIE to {
                    getCookie(
                        "CUID" to { AppPrivacyManager.getCuid() },
                        "TBBRAND" to { AppPrivacyManager.getModel() },
                        "cuid_galaxy2" to { AppPrivacyManager.getCuidGalaxy2() },
                        "SP_FW_VER" to { "3.340.42" },
                        "SG_FW_VER" to { "1.38.0" },
                        "BDUSS" to { AccountUtil.getBduss() },
                        "STOKEN" to { AccountUtil.getSToken() },
                        "BAIDU_WISE_UID" to { AppPrivacyManager.getClientId() },
                        "USER_JUMP" to { "-1" },
                        "BDUSS_BFESS" to { AccountUtil.getBduss() },
                        "BAIDUID" to { ClientUtils.baiduId },
                        "BAIDUID_BFESS" to { ClientUtils.baiduId },
                        "mo_originid" to { "2" },
                        "BAIDUZID" to { AppPrivacyManager.getZId() },
                    )
                }
            ),
            AddWebCookieInterceptor,
            CommonParamInterceptor(
                Param.BDUSS to { AccountUtil.getBduss() },
                Param.STOKEN to { AccountUtil.getSToken() },
            )
        )
    }

    val MINI_TIEBA_API: MiniTiebaApi by lazy {
        createJsonApi<MiniTiebaApi>(
            "http://c.tieba.baidu.com/",
            defaultCommonHeaderInterceptor,
            CommonHeaderInterceptor(
                Header.USER_AGENT to { "bdtb for Android 7.2.0.0" },
                Header.CUID to { UIDUtil.finalCUID },
                Header.CUID_GALAXY2 to { UIDUtil.finalCUID },
                "client_logid" to { "$initTime" }
            ),
            defaultCommonParamInterceptor + CommonParamInterceptor(
                Param.CUID to { UIDUtil.finalCUID },
                Param.CUID_GALAXY2 to { UIDUtil.finalCUID },
                Param.FROM to { "1021636m" },
                Param.CLIENT_VERSION to { "7.2.0.0" },
                Param.SUBAPP_TYPE to { "mini" }
            ),
            stParamInterceptor,
        )
    }

    val OFFICIAL_TIEBA_API: OfficialTiebaApi by lazy {
        createJsonApi<OfficialTiebaApi>(
            "http://c.tieba.baidu.com/",
            CommonHeaderInterceptor(
                Header.USER_AGENT to { "bdtb for Android 12.41.7.1" },
                Header.COOKIE to { "CUID=${AppPrivacyManager.getCuid()};ka=open;TBBRAND=${AppPrivacyManager.getModel()};BAIDUID=${ClientUtils.baiduId};" },
                Header.CUID to { AppPrivacyManager.getCuid() },
                Header.CUID_GALAXY2 to { AppPrivacyManager.getCuidGalaxy2() },
                Header.CUID_GID to { "" },
                Header.CUID_GALAXY3 to { AppPrivacyManager.getC3Aid() },
                Header.CLIENT_TYPE to { "2" },
                Header.CHARSET to { "UTF-8" },
                "client_logid" to { "$initTime" }
            ),
            defaultCommonParamInterceptor + CommonParamInterceptor(
                Param.ACTIVE_TIMESTAMP to { ClientUtils.activeTimestamp.toString() },
                Param.ANDROID_ID to { base64Encode(AppPrivacyManager.getAndroidId("000")) },
                Param.BAIDU_ID to { ClientUtils.baiduId },
                Param.BRAND to { AppPrivacyManager.getBrand() },
                Param.CMODE to { "1" },
                Param.CUID to { AppPrivacyManager.getCuid() },
                Param.CUID_GALAXY2 to { AppPrivacyManager.getCuidGalaxy2() },
                Param.CUID_GID to { "" },
                Param.EVENT_DAY to {
                    SimpleDateFormat("yyyyMdd", Locale.getDefault()).format(
                        Date(
                            System.currentTimeMillis()
                        )
                    )
                },
                Param.EXTRA to { "" },
                Param.FIRST_INSTALL_TIME to { AppPrivacyManager.getFirstInstallTime().toString() },
                Param.FRAMEWORK_VER to { "3340042" },
                Param.FROM to { "tieba" },
                Param.IS_TEENAGER to { "0" },
                Param.LAST_UPDATE_TIME to { AppPrivacyManager.getLastUpdateTime().toString() },
                Param.MAC to { "02:00:00:00:00:00" },
                Param.SAMPLE_ID to { ClientUtils.sampleId },
                Param.SDK_VER to { "2.34.0" },
                Param.START_SCHEME to { "" },
                Param.START_TYPE to { "1" },
                Param.SWAN_GAME_VER to { "1038000" },
                Param.CLIENT_VERSION to { "12.41.7.1" },
                Param.CUID_GALAXY3 to { AppPrivacyManager.getC3Aid() },
                Param.OAID to { AppPrivacyManager.getOaid() },
            ),
            stParamInterceptor,
        )
    }

    val OFFICIAL_PROTOBUF_TIEBA_API: OfficialProtobufTiebaApi by lazy {
        createProtobufApi<OfficialProtobufTiebaApi>(
            "https://tiebac.baidu.com/",
            CommonHeaderInterceptor(
                Header.CHARSET to { "UTF-8" },
                Header.CLIENT_TYPE to { "2" },
                Header.CLIENT_USER_TOKEN to { AccountUtil.getUid() },
                Header.COOKIE to { "CUID=${AppPrivacyManager.getCuid()};ka=open;TBBRAND=${AppPrivacyManager.getModel()};" },
                Header.CUID to { AppPrivacyManager.getCuid() },
                Header.CUID_GALAXY2 to { AppPrivacyManager.getCuidGalaxy2() },
                Header.CUID_GID to { "" },
                Header.CUID_GALAXY3 to { AppPrivacyManager.getC3Aid() },
                Header.USER_AGENT to { "bdtb for Android ${ClientVersion.TIEBA_V11.version}" },
                Header.X_BD_DATA_TYPE to { "protobuf" },
            ),
            defaultCommonParamInterceptor - Param.OS_VERSION + CommonParamInterceptor(
                Param.CUID to { AppPrivacyManager.getCuid() },
                Param.CUID_GALAXY2 to { AppPrivacyManager.getCuidGalaxy2() },
                Param.CUID_GID to { "" },
                Param.FROM to { "tieba" },
                Param.CLIENT_VERSION to { ClientVersion.TIEBA_V11.version },
                Param.CUID_GALAXY3 to { AppPrivacyManager.getC3Aid() },
                Param.OAID to { AppPrivacyManager.getOaid() },
            ),
            stParamInterceptor,
        )
    }

    val OFFICIAL_PROTOBUF_TIEBA_V12_API: OfficialProtobufTiebaApi by lazy {
        createProtobufApi<OfficialProtobufTiebaApi>(
            "https://tiebac.baidu.com/",
            CommonHeaderInterceptor(
                Header.CHARSET to { "UTF-8" },
                Header.CLIENT_TYPE to { "2" },
                Header.CLIENT_USER_TOKEN to { AccountUtil.getUid() },
                Header.COOKIE to {
                    getCookie(
                        "ka" to { "open" },
                        "CUID" to { AppPrivacyManager.getCuid() },
                        "TBBRAND" to { AppPrivacyManager.getModel() }
                    )
                },
                Header.CUID to { AppPrivacyManager.getCuid() },
                Header.CUID_GALAXY2 to { AppPrivacyManager.getCuidGalaxy2() },
                Header.CUID_GID to { "" },
                Header.CUID_GALAXY3 to { AppPrivacyManager.getC3Aid() },
                Header.USER_AGENT to { getUserAgent("tieba/${ClientVersion.TIEBA_V12.version}") },
                Header.X_BD_DATA_TYPE to { "protobuf" },
            ),
            stParamInterceptor,
        )
    }

    val OFFICIAL_PROTOBUF_TIEBA_POST_API: OfficialProtobufTiebaApi by lazy {
        createProtobufApi<OfficialProtobufTiebaApi>(
            "https://tiebac.baidu.com/",
            CommonHeaderInterceptor(
                Header.CHARSET to { "UTF-8" },
//                Header.CLIENT_TYPE to { "2" },
                Header.CLIENT_USER_TOKEN to { AccountUtil.getUid() },
                Header.COOKIE to {
                    getCookie(
                        "BAIDUZID" to { AppPrivacyManager.getZId() },
                        "ka" to { "open" },
                        "CUID" to { AppPrivacyManager.getCuid() },
                        "TBBRAND" to { AppPrivacyManager.getModel() }
                    )
                },
                Header.CUID to { AppPrivacyManager.getCuid() },
                Header.CUID_GALAXY2 to { AppPrivacyManager.getCuidGalaxy2() },
                Header.CUID_GID to { "" },
                Header.CUID_GALAXY3 to { AppPrivacyManager.getC3Aid() },
                Header.USER_AGENT to { getUserAgent("tieba/${ClientVersion.TIEBA_V12_POST.version}") },
                Header.X_BD_DATA_TYPE to { "protobuf" },
            ),
            defaultCommonParamInterceptor - Param.OS_VERSION + CommonParamInterceptor(
                Param.CLIENT_VERSION to { ClientVersion.TIEBA_V12_POST.version },
                Param.ACTIVE_TIMESTAMP to { ClientUtils.activeTimestamp.toString() },
                Param.ANDROID_ID to { base64Encode(AppPrivacyManager.getAndroidId("000")) },
                Param.BAIDU_ID to { ClientUtils.baiduId },
                Param.BRAND to { AppPrivacyManager.getBrand() },
                Param.CUID_GALAXY3 to { AppPrivacyManager.getC3Aid() },
                Param.CMODE to { "1" },
                Param.CUID to { AppPrivacyManager.getCuid() },
                Param.CUID_GALAXY2 to { AppPrivacyManager.getCuidGalaxy2() },
                Param.CUID_GID to { "" },
                Param.DEVICE_SCORE to { "${AppPrivacyManager.getDeviceScore()}" },
                Param.EVENT_DAY to {
                    SimpleDateFormat("yyyyMdd", Locale.getDefault()).format(
                        Date(
                            System.currentTimeMillis()
                        )
                    )
                },
                Param.EXTRA to { "" },
                Param.FIRST_INSTALL_TIME to { AppPrivacyManager.getFirstInstallTime().toString() },
                Param.FRAMEWORK_VER to { "3340042" },
                Param.FROM to { "tieba" },
                Param.IS_TEENAGER to { "0" },
                Param.LAST_UPDATE_TIME to { AppPrivacyManager.getLastUpdateTime().toString() },
                Param.MAC to { "02:00:00:00:00:00" },
                "naws_game_ver" to { "1038000" },
                Param.OAID to { AppPrivacyManager.getOaid() },
                "personalized_rec_switch" to { "1" },
                Param.SAMPLE_ID to { ClientUtils.sampleId },
                Param.SDK_VER to { "2.34.0" },
                Param.START_SCHEME to { "" },
                Param.START_TYPE to { "1" },
                Param.STOKEN to { AccountUtil.getSToken() },
                Param.Z_ID to { AppPrivacyManager.getZId() },
            ),
            stParamInterceptor,
        )
    }

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val SOFIRE_API: SofireApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://sofire.baidu.com/")
            .addCallAdapterFactory(DeferredCallAdapterFactory())
            .addCallAdapterFactory(FlowCallAdapterFactory.create())
            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(json.asConverterFactory())
            .addConverterFactory(gsonConverterFactory)
            .client(OkHttpClient.Builder().apply {
//                addInterceptor()
                connectionPool(connectionPool)
            }.build())
            .build()
            .create(SofireApi::class.java)
    }

    private inline fun <reified T : Any> createJsonApi(
        baseUrl: String,
        vararg interceptors: Interceptor
    ) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addCallAdapterFactory(DeferredCallAdapterFactory())
        .addCallAdapterFactory(FlowCallAdapterFactory.create())
        .addConverterFactory(NullOnEmptyConverterFactory())
        .addConverterFactory(json.asConverterFactory())
        .addConverterFactory(gsonConverterFactory)
        .client(OkHttpClient.Builder().apply {
            readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            interceptors.forEach {
                addInterceptor(it)
            }
            addInterceptor(DropInterceptor)
            addInterceptor(FailureResponseInterceptor)
            addInterceptor(ForceLoginInterceptor)
            addInterceptor(sortAndSignInterceptor)
            addInterceptor(ConnectivityInterceptor)
            connectionPool(connectionPool)
        }.build())
        .build()
        .create(T::class.java)

    private inline fun <reified T : Any> createProtobufApi(
        baseUrl: String,
        vararg interceptors: Interceptor
    ) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addCallAdapterFactory(DeferredCallAdapterFactory())
        .addCallAdapterFactory(FlowCallAdapterFactory.create())
        .addConverterFactory(NullOnEmptyConverterFactory())
        .addConverterFactory(WireConverterFactory.create())
        .client(OkHttpClient.Builder().apply {
            readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            interceptors.forEach {
                addInterceptor(it)
            }
            addInterceptor(DropInterceptor)
            addInterceptor(ProtoFailureResponseInterceptor)
            addInterceptor(ForceLoginInterceptor)
            addInterceptor(CookieInterceptor)
            addInterceptor(sortAndSignInterceptor)
            addInterceptor(ConnectivityInterceptor)
            connectionPool(connectionPool)
        }.build())
        .build()
        .create(T::class.java)
}