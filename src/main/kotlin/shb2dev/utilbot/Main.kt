package shb2dev.utilbot

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.user
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*
import kotlin.concurrent.schedule

const val SHB2_GUILD_ID = 1222791513801625630

suspend fun main() {
    val kord = Kord(File("token").readText())

    kord.on<MessageCreateEvent> {
        println(message.data.content)
        //DISBOARD君でだけ動くようにする
        if(message.data.author.id.toString() != "302050872383242240") return@on
        if (message.fetchMessage().embeds[0].description?.contains("表示順をアップしたよ") == true) {
            val bumpedUser = message.fetchMessage().interaction?.user?.id
            message.channel.createEmbed {
                title = "BUMPされました！"
                description = "2時間後に通知します！"
            }

            Timer().schedule(2 * 1000 * 3600) {
                runBlocking {
                    message.channel.createEmbed {
                        title = "2時間経ったよ！"
                        description = "<@${bumpedUser}>さん！\n2時間経ったのでまたBUMP出来ますよ！>"
                    }
                }
            }
        }
    }

    kord.createGuildChatInputCommand(Snowflake(SHB2_GUILD_ID),"icon", "ユーザーのアイコンを取得します") {
        user("user", "Username") {
            required = true
        }
        integer("size", "アイコンのサイズ")
    }

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val response = interaction.deferPublicResponse()
        val command = interaction.command
        val user = command.users["user"]!!
        val size = command.integers["size"] ?: 128

        response.respond {
            content = "${user.avatar?.cdnUrl?.toUrl()}?size=$size"
        }
    }

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}