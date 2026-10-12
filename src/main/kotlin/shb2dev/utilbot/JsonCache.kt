package shb2dev.utilbot

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.edit
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.exception.EntityNotFoundException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class JsonCache(var bumpData: BumpData? = null, var ruleData: RuleData? = null) {
    suspend fun modifyRuleMessage(channel: GuildMessageChannelBehavior) {
        val ruleId = CacheManager.cache.ruleData?.ruleMessageID ?: return sendRuleMessage(channel)
        when (val message = channel.getMessage(Snowflake(ruleId))) {
            EntityNotFoundException -> {
                Main.Logger.warn("sent message was is found. resend")
                sendRuleMessage(channel)
            }
            else -> {
                if (message.content != CacheManager.cache.ruleData!!.getRuleMessage()) {
                    message.edit {
                        content = CacheManager.cache.ruleData!!.getRuleMessage()
                    }
                }
            }
        }

    }
    private suspend fun sendRuleMessage(channel: GuildMessageChannelBehavior) {
        val ruleMessage = RuleData("", 0u).getRuleMessage()
        val message = channel.createMessage(ruleMessage)

        message.addReaction(ReactionEmoji.Unicode("\u2705")) // ✅
        message.addReaction(ReactionEmoji.Unicode("\u274C")) // ❌

        CacheManager.cache.ruleData = RuleData(ruleMessage, message.id.value)
        CacheManager.save()
    }

    fun calculateDelay(nowTime: Long, defaultTime: Long = 7200000) =  if(bumpData == null) Failure(Exception("Cannot get bumpedTimeStamp from JSON.")) else Success(
        bumpData!!.bumpedTimeStamp + defaultTime - nowTime)

    sealed class Result<T>
    data class Success<T>(val value: T): Result<T>()
    data class Failure<T>(val reason: Exception): Result<T>()

    @Serializable
    data class BumpData(var user: ULong, var bumpedTimeStamp: Long, var channelID: ULong)

    @Serializable
    data class RuleData(private val ruleMessage: String?, var ruleMessageID: ULong, val allowedUsers: List<ULong> = listOf(1084318050976796832u, 684622065197776937u),) {
        fun getRuleMessage(): String = ruleMessage ?: """
                1,当たり前ですが法に触れるようなことはやめてください
                2,ディスコードの規約をちゃんと守ってください
                3,人を不快にするような文や行動、不適切な言葉、発言はやめましょう
                4,雑談なので長文送るのは控えましょう
                5,運営がルールに反してると判断したらタイムアウト・キック・banします
                6,みんなと仲良く遊んでね
                7,政治的な発言や宗教関係は <#1257908426651078717>  <#1257911597918191646> でしてください(ここなら言論統制をしません！！！)
                8, #誰でも広告ちゃんねる！！！！ はサーバーで遊んだ向けの人に設置しています！遊んでいないのに**広告のみ**流している人は**BANやKickなどの対象です!**
                # 以上のルールはちゃんと守ろう！！！
                同意するなら✅を同意しないなら❌をおしてください
                更新日 2024/12/29
            """.trimIndent()
    }
}

object CacheManager {
    private val file = File("data/cache.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    var cache: JsonCache = JsonCache()
        private set

    fun load() {
        if (file.exists()) {
            cache = json.decodeFromString(file.readText())
        }
    }

    fun save() {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(cache))
    }

    fun initialize() {
        if (!file.exists()) {
            save()
        }
        load()
    }
}

