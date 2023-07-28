package com.naoido.command

import com.naoido.JDACore.Companion.jda
import com.naoido.enum.Fortunes
import com.naoido.logger
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.awt.Color
import java.util.*
import kotlin.collections.HashMap

class Omikuji: Command{
    companion object {
        val result: HashMap<String, Result> = HashMap();
        val notificationUsers: MutableList<Long> = mutableListOf(279583321766494208L);

        private fun setSchedule() {
            logger.info("Created a new task omikuji.");
            val task = object: TimerTask() {
                override fun run() {
                    setSchedule();
                    result.clear();
                    for (userId in notificationUsers) {
                        sendOmikujiResult(userId);
                    }
                }
            }
            val calendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE) + 1, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Timer(false).schedule(task, calendar.time);
        }

        fun drawOmikuji(userId: Long) {
            if (!result.containsKey(userId.toString())) {
                val luck: String = when ((0..99).random()) {
                    99 -> Fortunes.DAIDAIKICHI.displayName;
                    in 76..98 -> Fortunes.DAIKICHI.displayName;
                    in 66..75 -> Fortunes.CHUUKICHI.displayName;
                    in 53..65 -> Fortunes.SHOUKICHI.displayName;
                    in 29..52 -> Fortunes.KICHI.displayName;
                    in 10..28 -> Fortunes.SUEKICHI.displayName;
                    in 1..9 -> Fortunes.KYOU.displayName;
                    else -> Fortunes.DAIKYOU.displayName;
                }
                val subLuck: List<Int> = listOf(
                    (0..3).random(),
                    (0..3).random(),
                    (0..3).random(),
                    (0..3).random(),
                    (0..3).random(),
                    (0..3).random()
                );
                result[userId.toString()] = Result(luck, subLuck);
            }
        }

        fun sendOmikujiResult(userId: Long) {
            drawOmikuji(userId);
            val name: String = jda.getUserById(userId)?.name?: "null";
            val embed: MessageEmbed = result[userId.toString()]?.getEmbed(name) ?: EmbedBuilder().setTitle("Error").build();
            jda.getUserById(userId)?.openPrivateChannel()?.complete()?.sendMessageEmbeds(embed)?.queue();
        }
    }

    init {
        // Set schedule omikuji
        setSchedule();
    }

    override val name: String = "omikuji";

    override val option: SlashCommandData = Commands.slash(name,"一日の運勢を占えます。");

    override fun commandHandle(event: SlashCommandInteractionEvent) {
        if (!result.containsKey(event.user.id)) {
            drawOmikuji(event.user.idLong);
        }

        result[event.user.id]?.getEmbed(event.user.name)?.let { event.replyEmbeds(it).queue() }
    }

    data class Result(val luck: String, val subLuck: List<Int>) {
        fun getEmbed(name: String): MessageEmbed {
            return EmbedBuilder().setTitle("今日の${name}さんの運勢は...")
                .setColor(Color(0x38A1A1))
                .addField("運勢", this.luck, false)
                .addField("恋愛", subLuckToString(this.subLuck[0], this.luck), true)
                .addField("待人", subLuckToString(this.subLuck[1], this.luck), true)
                .addField("勉強", subLuckToString(this.subLuck[2], this.luck), true)
                .addField("健康", subLuckToString(this.subLuck[3], this.luck), true)
                .addField("旅行", subLuckToString(this.subLuck[4], this.luck), true)
                .addField("商売", subLuckToString(this.subLuck[5], this.luck), true)
                .build();
        }

        private fun subLuckToString(subLuck: Int, luck: String): String {
            return when(luck) {
                "大大吉" -> "◎";
                "大凶" -> "×";
                else -> when(subLuck) {
                    0 -> "×";
                    1 -> "〇";
                    2 -> "◎";
                    else -> "△";
                }
            }
        }
    }
}

