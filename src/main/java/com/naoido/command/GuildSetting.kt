package com.naoido.command

import com.naoido.model.GuildSettings
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

class GuildSetting: Command {

    override fun commandHandle(event: SlashCommandInteractionEvent) {
        when(event.subcommandName) {
            "prefix" -> {
                event.guild?.idLong?.let { guildId ->
                    event.getOption("prefix")?.asString?.let { prefix ->
                            val oldPrefix: String = GuildSettings.INSTANCE.getPrefix(guildId);

                            GuildSettings.INSTANCE.setPrefix(guildId, prefix);
                            event.ephemeralReplay("Prefixを`${oldPrefix}`から`${prefix}`に変更しました。").queue();
                    }?: event.ephemeralReplay("オプション`[prefix]`を取得できませんでした。")
                }?: event.ephemeralReplay("`[GuildId]`を取得できませんでした。").queue();
            }

            "twitter" -> {
                event.guild?.idLong?.let {guildId ->
                    val setting: GuildSettings.Setting = GuildSettings.INSTANCE.getGuildSetting(guildId);

                    event.getOption("boolean")?.asBoolean?.let { isEnable ->
                        val enable: String = if (isEnable) "有効" else "無効";

                        event.getOption("setting")?.asString?.let {name ->
                            when (name) {
                                "auto_reaction" -> {
                                    setting.twitter.allowAutoAddReaction = isEnable;
                                    event.ephemeralReplay("`auto_reaction`を${enable}にしました。").queue();
                                }
                                "send_to_dm" -> {
                                    setting.twitter.isSendToDm = isEnable;
                                    event.ephemeralReplay("`send_to_dm`を${enable}にしました。").queue();
                                }
                                else -> {
                                    event.ephemeralReplay("`[オプション名]`を取得できませんでした。").queue();
                                }
                            }
                        }?: event.ephemeralReplay("オプション`[setting]`を取得できませんでした。").queue();
                    }?: event.ephemeralReplay("オプション`[boolean]`を取得できませんでした。").queue();
                }?: event.ephemeralReplay("`[GuildId]`を取得できませんでした。").queue();
            }
        }
    }

    override val name: String = "setting";

    override val option: SlashCommandData = Commands.slash(name, "botの設定ができます。")
        .addSubcommands(SubcommandData("prefix", "prefixの変更ができます。")
            .addOptions(
                OptionData(OptionType.STRING, "prefix", "prefixを入力").setRequired(true)
            ),
            SubcommandData("twitter", "twitter関連の設定の変更ができます。")
                .addOptions(
                    OptionData(OptionType.STRING, "setting", "設定の項目を選択できます。")
                        .addChoice("auto_reaction", "auto_reaction")
                        .addChoice("send_to_dm", "send_to_dm").setRequired(true),
                    OptionData(OptionType.BOOLEAN, "boolean", "有効/無効").setRequired(true)
                )
            )
        .setGuildOnly(true).setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
}