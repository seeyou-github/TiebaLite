package com.huanchengfly.tieba.post.api

import android.content.Context
import android.os.Build
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.models.OAID
import com.huanchengfly.tieba.post.api.models.protos.AppPosInfo
import com.huanchengfly.tieba.post.api.models.protos.CommonRequest
import com.huanchengfly.tieba.post.api.models.protos.frsPage.AdParam
import com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.body.MyMultipartBody
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.CacheUtil.base64Encode
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.DeviceUtils
import com.huanchengfly.tieba.post.utils.AppPrivacyManager
import com.huanchengfly.tieba.post.utils.UIDUtil
import com.squareup.wire.Message
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val BOUNDARY = "--------7da3d81520810*"

fun buildProtobufRequestBody(
    data: Message<*, *>,
    clientVersion: ClientVersion = ClientVersion.TIEBA_V11,
    needSToken: Boolean = true,
): MyMultipartBody {
    return MyMultipartBody.Builder(BOUNDARY)
        .apply {
            setType(MyMultipartBody.FORM)
            if (clientVersion != ClientVersion.TIEBA_V12 && clientVersion != ClientVersion.TIEBA_V12_POST) {
                addFormDataPart(Param.CLIENT_VERSION, clientVersion.version)
            }
            if (needSToken) {
                val sToken = AccountUtil.getSToken()
                if (sToken != null) addFormDataPart(Param.STOKEN, sToken)
            }
            addFormDataPart("data", "file", data.encode().toRequestBody())
        }
        .build()
}

fun buildAdParam(
    load_count: Int = 0,
    refresh_count: Int = 4,
    yoga_lib_version: String? = "1.0"
): AdParam {
    return AdParam(
        load_count = load_count,
        refresh_count = refresh_count,
        yoga_lib_version = yoga_lib_version
    )
}

fun buildAppPosInfo(): AppPosInfo {
    return AppPosInfo(
        addr_timestamp = 0L,
        ap_connected = true,
        ap_mac = "02:00:00:00:00:00",
        asp_shown_info = "",
        coordinate_type = "BD09LL"
    )
}

fun buildCommonRequest(
    context: Context = App.INSTANCE,
    clientVersion: ClientVersion = ClientVersion.TIEBA_V11,
    bduss: String? = null,
    stoken: String? = null,
    tbs: String? = null,
): CommonRequest = when (clientVersion) {
    ClientVersion.TIEBA_V11 -> {
        CommonRequest(
            BDUSS = bduss ?: AccountUtil.getBduss(),
            _client_id = AppPrivacyManager.getClientId(),
            _client_type = 2,
            _client_version = clientVersion.version,
            _os_version = AppPrivacyManager.getOsVersion(),
            _phone_imei = AppPrivacyManager.getPhoneImei(context),
            _timestamp = System.currentTimeMillis(),
            brand = AppPrivacyManager.getBrand(),
            c3_aid = AppPrivacyManager.getC3Aid(),
            cuid = AppPrivacyManager.getCuid(),
            cuid_galaxy2 = AppPrivacyManager.getCuidGalaxy2(),
            cuid_gid = "",
            from = "1024324o",
            is_teenager = 0,
            lego_lib_version = "3.0.0",
            model = AppPrivacyManager.getModel(),
            net_type = AppPrivacyManager.getNetType().toIntOrNull() ?: 1,
            oaid = AppPrivacyManager.getOaid(),
            pversion = "1.0.3",
            sample_id = ClientUtils.sampleId,
            stoken = stoken ?: AccountUtil.getSToken(),
        )
    }

    ClientVersion.TIEBA_V12 -> {
        CommonRequest(
            BDUSS = AccountUtil.getBduss(),
            _client_id = AppPrivacyManager.getClientId(),
            _client_type = 2,
            _client_version = clientVersion.version,
            _os_version = AppPrivacyManager.getOsVersion(),
            _phone_imei = AppPrivacyManager.getPhoneImei(context),
            _timestamp = System.currentTimeMillis(),
            active_timestamp = ClientUtils.activeTimestamp,
            android_id = base64Encode(AppPrivacyManager.getAndroidId("000")),
            brand = AppPrivacyManager.getBrand(),
            c3_aid = AppPrivacyManager.getC3Aid(),
            cmode = 1,
            cuid = AppPrivacyManager.getCuid(),
            cuid_galaxy2 = AppPrivacyManager.getCuidGalaxy2(),
            cuid_gid = "",
            event_day = SimpleDateFormat("yyyyMdd", Locale.getDefault()).format(
                Date(
                    System.currentTimeMillis()
                )
            ),
            extra = "",
            first_install_time = AppPrivacyManager.getFirstInstallTime(),
            framework_ver = "3340042",
            from = "1020031h",
            is_teenager = 0,
            last_update_time = AppPrivacyManager.getLastUpdateTime(),
            lego_lib_version = "3.0.0",
            model = AppPrivacyManager.getModel(),
            net_type = AppPrivacyManager.getNetType().toIntOrNull() ?: 1,
            oaid = "",
            personalized_rec_switch = 1,
            pversion = "1.0.3",
            sample_id = ClientUtils.sampleId,
            scr_dip = AppPrivacyManager.getScrDip(),
            scr_h = AppPrivacyManager.getScrH(),
            scr_w = AppPrivacyManager.getScrW(),
            sdk_ver = "2.34.0",
            start_scheme = "",
            start_type = 1,
            stoken = AccountUtil.getSToken(),
            swan_game_ver = "1038000",
            user_agent = getUserAgent("tieba/${clientVersion.version}"),
            z_id = AppPrivacyManager.getZId()
        )
    }

    ClientVersion.TIEBA_V12_POST -> {
        CommonRequest(
            BDUSS = AccountUtil.getBduss(),
            _client_id = AppPrivacyManager.getClientId(),
            _client_type = 2,
            _client_version = clientVersion.version,
            _os_version = AppPrivacyManager.getOsVersion(),
            _phone_imei = AppPrivacyManager.getPhoneImei(context),
            _timestamp = System.currentTimeMillis(),
            active_timestamp = ClientUtils.activeTimestamp,
            android_id = AppPrivacyManager.getAndroidId("000"),
            applist = "",
            brand = AppPrivacyManager.getBrand(),
            c3_aid = AppPrivacyManager.getC3Aid(),
            cmode = 1,
            cuid = AppPrivacyManager.getCuid(),
            cuid_galaxy2 = AppPrivacyManager.getCuidGalaxy2(),
            cuid_gid = "",
            device_score = AppPrivacyManager.getDeviceScore().toString(),
            event_day = SimpleDateFormat("yyyyMdd", Locale.getDefault()).format(
                Date(
                    System.currentTimeMillis()
                )
            ),
            extra = "",
            first_install_time = AppPrivacyManager.getFirstInstallTime(),
            framework_ver = "3340042",
            from = "1020031h",
            is_teenager = 0,
            last_update_time = AppPrivacyManager.getLastUpdateTime(),
            lego_lib_version = "3.0.0",
            model = AppPrivacyManager.getModel(),
            net_type = AppPrivacyManager.getNetType().toIntOrNull() ?: 1,
            oaid = AppPrivacyManager.getOaid(),
            personalized_rec_switch = 1,
            pversion = "1.0.3",
            q_type = 0,
            sample_id = ClientUtils.sampleId,
            scr_dip = AppPrivacyManager.getScrDip(),
            scr_h = AppPrivacyManager.getScrH(),
            scr_w = AppPrivacyManager.getScrW(),
            sdk_ver = "2.34.0",
            start_scheme = "",
            start_type = 1,
            stoken = AccountUtil.getSToken(),
            swan_game_ver = "1038000",
            tbs = tbs,
            user_agent = getUserAgent("tieba/${clientVersion.version}"),
            z_id = AppPrivacyManager.getZId()
        )
    }
}