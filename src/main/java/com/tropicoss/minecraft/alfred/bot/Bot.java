package com.tropicoss.minecraft.alfred.bot;

import com.tropicoss.minecraft.alfred.PlayerInfoFetcher;
import com.tropicoss.minecraft.alfred.config.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

import static com.tropicoss.minecraft.alfred.Alfred.LOGGER;

public class Bot {

    private static final Bot instance;

    static {
        try {
            instance = new Bot();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private final JDA BOT;

    private final TextChannel CHANNEL;

    private Bot() throws InterruptedException {
        try {
            BOT = JDABuilder.createDefault(Config.Bot.token)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new Listeners())
                    .build()
                    .awaitReady();

            CHANNEL = BOT.getTextChannelById(Config.Bot.channel);
        } catch (Exception e) {
            switch (e.getClass().getSimpleName()) {
                case "InvalidTokenException":
                    LOGGER.error("Invalid bot token. Please check your config file.");
                    break;
                case "IllegalArgumentException":
                    LOGGER.error("Invalid bot channel. Please check your config file.");
                    break;
                default:
                    LOGGER.error("Error starting bot: " + e.getMessage());
                    break;
            }
            throw e;
        }
    }

    public static Bot getInstance() {
        return instance;
    }

    public void onStartUp() {
        CHANNEL
                .sendMessageEmbeds(
                        new EmbedBuilder()
                                .setAuthor(Config.Generic.name)
                                .setDescription("Server has started!")
                                .setFooter(Config.Generic.name)
                                .setTimestamp(Instant.now())
                                .setColor(39129)
                                .build())
                .queue();
    }

    public void onShutDown() {
        CHANNEL
                .sendMessageEmbeds(
                        new EmbedBuilder()
                                .setAuthor(Config.Generic.name)
                                .setDescription("Server has shut down!")
                                .setFooter(Config.Generic.name)
                                .setTimestamp(Instant.now())
                                .setColor(39129)
                                .build())
                .queue();

        try {
            BOT.shutdownNow();
        } catch (Exception e) {
            switch (e.getClass().getSimpleName()) {
                case "InterruptedIOException":
                    LOGGER.error("Bot is shutting down.");
                    break;
                default:
                    LOGGER.error("Error shutting down bot: " + e.getMessage());
                    break;
            }
        }
    }

    public void sendEmbedMessage(
            String message, @Nullable PlayerInfoFetcher.Profile profile, String ServerName) {

        if (CHANNEL == null) {
            LOGGER.error("Chat channel not found. Please check your config file.");
            return;
        }

        EmbedBuilder builder =
                new EmbedBuilder()
                        .setDescription(message)
                        .setFooter(ServerName)
                        .setTimestamp(Instant.now())
                        .setColor(39129);

        if (profile != null) {
            builder.setAuthor(
                    profile.data.player.username,
                    String.format("https://namemc.com/profile/%s", profile.data.player.username),
                    profile.data.player.avatar);
        } else {
            builder.setAuthor(ServerName);
        }

        CHANNEL.sendMessageEmbeds(builder.build()).queue();
    }
}
