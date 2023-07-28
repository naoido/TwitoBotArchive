package com.naoido.command

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.naoido.JDACore.Companion.jda
import com.naoido.JDACore.Companion.sendMessageToOwner
import com.naoido.enum.Emojis
import com.naoido.logger
import com.naoido.model.GuildSettings
import com.naoido.sql.SqlCore
import com.naoido.sql.enum.Table
import com.twitter.clientlib.TwitterCredentialsBearer
import com.twitter.clientlib.api.TwitterApi
import com.twitter.clientlib.model.Get2TweetsIdResponse
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import okio.IOException
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.ResultSet
import java.util.Properties
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.io.path.name

class Twitter: Command {
    companion object {
        private val BEARER_TOKEN: String;

        init {
            val sdkPropertyPath: Path = Paths.get("sdk.properties");
            val properties: Properties = Properties();
            properties.load(this::class.java.getResourceAsStream("/token.properties"));
            this.BEARER_TOKEN = properties.getProperty("twitter_bearer");
            findAndCreate(sdkPropertyPath);
        }

        val INSTANCE: Twitter by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Twitter() };
        private val twitterApi: TwitterApi = TwitterApi(TwitterCredentialsBearer(BEARER_TOKEN));
        private val expansions: Set<String> = HashSet(listOf("attachments.media_keys"));
        private val mediaFields: Set<String> = HashSet(listOf("url", "type", "duration_ms", "variants"));
        private val tweetsFields: Set<String> = HashSet(listOf("possibly_sensitive", "author_id"));
        private const val TWEET_URI_PATTERN: String = "https?://(?:mobile\\.)?(?:www\\.)?twitter\\.com/\\w{1,15}/status/\\d{16,19}(?:\\?.*)?";
        private const val OEMBED_BASE_URL: String = "https://publish.twitter.com/oembed";
        private const val USER_ID_BASE_URL = "https://twitter.com/i/user/";

        fun isTweetURL(url: String): Boolean {
            return url.matches(Regex("^${TWEET_URI_PATTERN}$"));
        }

        fun containsTweetUrl(text: String): Boolean {
            return Pattern.compile(TWEET_URI_PATTERN).matcher(text).find();
        }

        private fun getTweetUrl(text: String): String? {
            return Regex(TWEET_URI_PATTERN).find(text)?.groupValues?.get(0);
        }

        private fun getTweetId(url: String): String? {
            val matcher: Matcher = Regex("\\d{16,19}").toPattern().matcher(url);
            if (matcher.find()) return matcher.group();
            return null;
        }

        private fun getUserId(username: String): String {
            return twitterApi.users().findUserByUsername(username).execute().data?.id ?: "null";
        }

        private fun getUserLinkById(userId: String): String {
            return USER_ID_BASE_URL + userId;
        }

        fun getMediaUrls(url: String, userId: String): Pair<MutableList<String>, Boolean> {
            val tweetId: String? = getTweetId(url);
            tweetId?.let {
                val response: ResultSet = SqlCore.where(Table.TWEET_DATA, null, Pair("tweet_id", tweetId));
                val uuid: String = UUID.randomUUID().toString();

                if (response.next()) {
                    val urls: MutableList<String> = ArrayList();
                    var sensitive: Boolean;
                    val tweetUuid: String = response.getString("uuid");

                    logger.info("Found a media data [${tweetId}]");
                    do {
                        urls.add(response.getString("media_url"));
                        sensitive = response.getBoolean("nsfw");
                    } while ((response.next()));
                    if (!SqlCore.where(Table.HISTORIES, null, Pair("user_id", userId), Pair("uuid", tweetUuid)).next()) {
                        SqlCore.values(Table.HISTORIES, Table.values(
                            userId,
                            tweetUuid,
                            SqlCore.getTimestampNow()
                        ));
                    } else {
                        SqlCore.update(Table.HISTORIES, listOf(Pair("timestamp",SqlCore.getTimestampNow())),
                            Pair("user_id", userId),
                            Pair("uuid", tweetUuid)
                        );
                    }
                    response.close();
                    return Pair(urls, sensitive);
                }

                try {
                    val tweetResponse: Get2TweetsIdResponse = twitterApi.tweets().findTweetById(tweetId)
                        .expansions(expansions)
                        .mediaFields(mediaFields)
                        .tweetFields(tweetsFields)
                        .execute();
                    val node: JsonNode = ObjectMapper().readTree(tweetResponse.toJson());
                    val mediaNode: String = node.path("includes").path("media").toString();
                    val dataNode: JsonNode = node.path("data");
                    val sensitive: Boolean = dataNode.path("possibly_sensitive").asBoolean();
                    val authorId: String = dataNode.path("author_id").asText();
                    val medias: List<Media> = jacksonObjectMapper().readValue(mediaNode);
                    val urls: MutableList<String> = ArrayList();

                    for (media in medias) {
                        val mediaUrl: String = media.getMediaUrl();
                        val mediaKey: String = getMediaKey(mediaUrl);
                        SqlCore.values(Table.TWEET_DATA, Table.values(
                            uuid,
                            authorId,
                            tweetId,
                            mediaKey,
                            getMediaId(mediaUrl),
                            mediaUrl,
                            sensitive,
                            media.getTypeInt()));
                        urls.add(media.getMediaUrl());
                    }

                    SqlCore.values(Table.HISTORIES, Table.values(
                        userId,
                        uuid,
                        SqlCore.getTimestampNow()
                    ));

                    logger.info("Get status by \"$tweetId\"");
                    logger.info("Get media urls $urls");

                    return Pair(urls, sensitive);
                } catch (e: IOException) {
                    e.printStackTrace();
                    return Pair(mutableListOf("エラー"), false);
                }
            }
            return Pair(mutableListOf("エラー"), false);
        }

        private fun getMediaKey(url: String): String {
            return Regex("[\\w-]{8,20}").find(url.substringAfterLast("/"))?.groupValues?.get(0)?: let {
                sendMessageToOwner("worning", "$url に '\\w{8,20}' がマッチしませんでした。");
                throw IllegalArgumentException("");
            };
        }

        private fun getMediaId(url: String): Long? {
            return Regex("\\d{15,21}").find(url)?.groupValues?.get(0)?.toLong();
        }

        fun hasMedia(url: String): Boolean {
            try {
                URL("${OEMBED_BASE_URL}?url=${url}").openStream().use {stream ->
                    BufferedReader(InputStreamReader(stream)).use {
                        return it.readLine().contains("pic.twitter.com");
                    }
                };
            } catch (e: FileNotFoundException) {
                e.printStackTrace();
                return false;
            } catch (e: IOException) {
                logger.warning("maybe response code: 403");
                return true;
            }
        }

        fun reactionHandle(event: MessageReactionAddEvent) {
            if (GuildSettings.INSTANCE.getGuildSetting(event.guild.idLong).twitter.allowAutoAddReaction.not() ||
                !event.retrieveMessage().complete().retrieveReactionUsers(Emojis.ARROW_DOWN.getEmoji()).complete().contains(jda.selfUser)) {
                return;
            }
            getTweetUrl(event.retrieveMessage().complete().contentDisplay)?: return;
            event.user?.let { sendMedia(event.retrieveMessage().complete(), it, true) };
        }

        private fun sendMedia(message: Message, user: User, isGuild: Boolean) {
            getMediaUrls(message.contentDisplay, user.id).let{
                //if tweets are sensitive, sending it to DM
                if (it.second) {
                    if (isGuild && !message.channel.asTextChannel().isNSFW) {
                        message.addReaction(Emojis.UNDERAGE.getEmoji()).complete();
                        user.openPrivateChannel().complete().sendMessage("`このツイートにセンシティブな内容が含まれていたのでDMに送信しました。`")
                            .addContent("\n> ${message.jumpUrl}\n")
                            .addContent(it.first.joinToString(" "))
                            .mentionRepliedUser(false)
                            .queue();
                    } else {
                        message.reply(it.first.joinToString(" ")).mentionRepliedUser(false).queue();
                    }
                } else {
                    message.reply(it.first.joinToString(" "))
                        .mentionRepliedUser(false)
                        .queue();
                    message.removeReaction(Emojis.ARROW_DOWN.getEmoji()).complete();
                }
            }
        }

        private fun findAndCreate(vararg paths: Path) {
            for (path in paths) {
                try {
                    if (!Files.exists(path)) {
                        Files.createFile(path);
                        logger.info("Created a new file [${path.name}]");
                    } else {
                        logger.info("Found a file [${path.name}]");
                    }
                } catch (e: IOException) {
                    e.printStackTrace();
                }
            }
        }
    }

    override val name: String = "twitter";

    override val option: SlashCommandData
        get() = Commands.slash(name, "twitter関連")
            .addSubcommands(SubcommandData("media", "メディアの取得")
                .addOptions(OptionData(OptionType.STRING, "url", "Mediaを取得するTweetURL")
                    .setRequired(true)),
                SubcommandData("id", "ユーザーIDの取得")
                    .addOptions(OptionData(OptionType.STRING, "id", "取得するユーザーのid")
                        .setRequired(true))
            );

    override fun commandHandle(event: SlashCommandInteractionEvent) {
        when(event.subcommandName) {
            "media" -> {
                val url: String = event.getOption("url")?.asString ?: "";
                if (isTweetURL(url)) {
                    logger.info("Found a tweet url \"${url}\"");
                    event.reply(getMediaUrls(url, event.user.id).first.joinToString(" ")).setEphemeral(false).complete();
                } else {
                    event.reply("URLを認識できませんでした。").setEphemeral(true).complete();
                }
            }
            "id" -> {
                event.getOption("id")?.asString?.let {
                    event.reply(getUserLinkById(getUserId(it))).queue();
                }?: run {
                    event.ephemeralReplay("idが認識できませんでした。").queue();
                }
            }
        }
    }

    override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
        return this.isPrefixCommand(command);
    }

    override fun isPrefixCommand(command: String): Boolean {
        return containsTweetUrl(command);
    }

    override fun prefixHandle(event: MessageReceivedEvent) {
        val url: String = getTweetUrl(event.message.contentRaw)?: return;

        if (hasMedia(url)) {
            if (event.isFromGuild.not()) {
                sendMedia(event.message, event.author, false);
                return;
            }
            event.message.addReaction(Emojis.ARROW_DOWN.getEmoji()).queue();
        }
    }

    data class Media(@JsonProperty("duration_ms") val durationMs: Long?,
                     @JsonProperty("url") val url: String?,
                     @JsonProperty("media_key") val mediaKey: String?,
                     @JsonProperty("type") val type: String?,
                     @JsonProperty("variants") val variants: List<Variant>?) {

        fun getMediaUrl(): String {
            return when(this.type) {
                "photo" -> "${this.url}?name=orig";
                "animated_gif" -> this.variants!![0].url;
                "video" -> {
                    var bitRate: Long = 0;
                    var url: String = "";
                    for (variant in this.variants!!) {
                        if (variant.contentType.equals("video/mp4") && variant.bitRate != null && variant.bitRate > bitRate) {
                            bitRate = variant.bitRate;
                            url = variant.url;
                        }
                    }
                    return url;
                };
                else -> "null";
            }
        }

        fun getTypeInt(): Int {
            return when(this.type) {
                "photo" -> 1
                "video" -> 2
                "animated_gif" -> 3
                else -> throw IllegalArgumentException("Not found media type [${this.type}].");
            }
        }

        data class Variant(@JsonProperty("bit_rate") val bitRate: Long?,
                           @JsonProperty("content_type") val contentType: String?,
                           @JsonProperty("url") val url: String);
    }
}