package shb2dev.utilbot

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.user
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds


object Main {
    const val TOKEN_ENV_NAME = "DISCORD_TOKEN"
    const val DISBOARD_BOT_ID = "302050872383242240"
    const val SHB2_GUILD_ID: ULong = 1222791513801625630u
    private lateinit var kord: Kord

    val Logger: Logger = LoggerFactory.getLogger("shb2dev.utilbot")

    private fun loadOldBumpData() {
        kord.launch {
            when(val delay = CacheManager.cache.calculateDelay(System.currentTimeMillis())) {
                is JsonCache.Success<Long> -> {
                    Logger.debug("Bump cooldown Remining...: {}", delay.value.milliseconds.toString())

                    if (delay.value < 0) {
                        Logger.warn("Bump cooldown is finished.")
                        return@launch
                    }

                    delay(delay.value.milliseconds)

                    val channel = CacheManager.cache.bumpData?.channelID?.let { MessageChannelBehavior(Snowflake(it), kord) }
                    if (channel == null) {
                           Logger.error("Cannot get channel")
                    } else {
                        sendBumpMessage(channel, CacheManager.cache.bumpData?.user?: 0UL)

                    }
                }
                is JsonCache.Failure -> {
                    Logger.error("Failed to get timestamp data from JSON.")
                }
            }

        }
    }

    private suspend fun sendBumpMessage(channel: MessageChannelBehavior, bumpedUser: ULong) {
        channel.createEmbed {
            title = "2時間経ったよ！"
            description = "<@${bumpedUser}>さん！\n2時間経ったのでまたBUMP出来ますよ！"
        }
    }
    suspend fun run(serverID: ULong) {
        Logger.info("Initializing")

        kord = Kord(System.getenv(TOKEN_ENV_NAME) ?: throw IllegalArgumentException("Environment variable '${TOKEN_ENV_NAME}' is missing.\nYou should be set '${TOKEN_ENV_NAME}' environment."))

        Logger.info("Load old bump data.")
        loadOldBumpData()

        val serverSnowflake = Snowflake(serverID)

        Logger.debug("bot start with {}", serverSnowflake)

        kord.on<MessageCreateEvent> {
            //DISBOARD君でだけ動くようにする
            if(message.data.author.id.toString() != DISBOARD_BOT_ID) return@on
            if (message.fetchMessage().embeds[0].description?.contains("表示順をアップしたよ") == true) {
                val bumpedUser = message.fetchMessage().interaction?.user?.id ?: return@on Logger.error("Cannot get User Information.")
                Logger.info("Bump called from $bumpedUser")
                message.channel.createEmbed {
                    title = "BUMPされました！"
                    description = "2時間後に通知します！"
                }
                val nowTime = message.timestamp.toEpochMilliseconds()
                CacheManager.cache.bumpData = JsonCache.BumpData(
                    bumpedUser.value,
                    nowTime,
                    message.channelId.value
                )
                CacheManager.save()

                kord.launch {
                    delay(2.hours)

                    sendBumpMessage(message.channel, bumpedUser.value)
                }
            }
        }
        kord.on<ReactionAddEvent> {
            // ルールについたリアクションでロールを付与するやつ
            if (messageId.value != CacheManager.cache.ruleData?.ruleMessageID) return@on

            when (emoji.name) {
                "\u2705" -> {
                    // TODO: ここのハードコードは多分直したほうが良いけど、どっちでも良さそう
                    user.asMember(serverSnowflake).addRole(Snowflake(1222792284790067220), "ルールに同意したため")
                    Logger.info("Add 'member' role to User {}", user.asUser().username)
                }
                "\u274C" -> {
                    user.asMember(serverSnowflake).addRole(Snowflake(1371847552948895744), "ルールに同意しなかったため")
                    Logger.info("Add 'disagree' role to User {}", user.asUser().username)
                }
            }

            // ✅: \u2705
            // ❌: \u274C
        }

        kord.createGuildChatInputCommand(serverSnowflake,"icon", "ユーザーのアイコンを取得します") {
            user("user", "Username") {
                required = true
            }
            integer("size", "アイコンのサイズ")
        }
        kord.createGuildChatInputCommand(serverSnowflake, "setuprule", "ルールチャンネルのセットアップをするよ")

        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            val command = interaction.command

            when(command.rootName) {
                "icon" -> {
                    val user = command.users["user"]!!
                    val size = command.integers["size"] ?: 128

                    interaction.deferPublicResponse().respond {
                        content = "${user.avatar?.cdnUrl?.toUrl()}?size=$size"
                    }
                }
                "setuprule" -> {
                    val author = interaction.user.id.value
                    Logger.info("{} is called setup rule command", author)

                    // cacheに存在していなければRuleData classの初期設定値を使用する
                    val messageContent = if ((CacheManager.cache.ruleData?.allowedUsers ?: JsonCache.RuleData("", 0u).allowedUsers).contains(author)) {
                        Logger.debug("function modifyRuleMessage is called.")
                        CacheManager.cache.modifyRuleMessage(interaction.channel)
                        "送信しました"
                    } else {
                        Logger.info("{} has not in allowed list.", author)
                        "許可されていません"
                    }
                    interaction.deferEphemeralResponse().respond {
                        content = messageContent
                    }


                }
            }
        }

        kord.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
        Logger.info("Bot Started.")
    }
}

suspend fun main() {
    CacheManager.initialize()

    val debugServerID: ULong? = System.getenv("DEBUG_SERVER_ID")?.toULong()
    Main.run(debugServerID ?: Main.SHB2_GUILD_ID)
}