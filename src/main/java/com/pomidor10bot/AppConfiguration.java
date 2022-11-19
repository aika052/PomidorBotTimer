package com.pomidor10bot;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.sql.DataSource;

@Configuration  // аннотация
public class AppConfiguration {
    // создаётся бот в след.бине
    @Bean   // вместо того что бы создавать каждый раз экземпляр класса.Используем один и тот же класс. Должен быть без сотояния в единственном экз.
    public PomidoroBot pomidoroBot(TimerDao timerDao) { return new PomidoroBot(timerDao); }
    @Bean
    public DataSource dataSource() {
        HikariConfig  hikariConfig = new HikariConfig ();
        hikariConfig.setJdbcUrl("jdbc:postgresql://localhost:49153/postgres");
        hikariConfig.setUsername("postgres");
        hikariConfig.setPassword("postgrespw");
        return  new HikariDataSource(hikariConfig);
    }
    @Bean
    public TimerDao timerDao(DataSource dataSource) { return new TimerDao(dataSource); }

    @Bean // регистрируем бота
    public TelegramBotsApi telegramBotsApi(PomidoroBot pomidoroBot) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        //удалили var pomidoroBot = new PomidoroBot(); потому что используем @Bean
        telegramBotsApi.registerBot(pomidoroBot);
        new Thread(() -> {
            try {
                pomidoroBot.checkTimer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).run();   // запускаем поток. есть ещё похожий .start()
        return telegramBotsApi;
    }
}
