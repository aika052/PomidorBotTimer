package com.pomidor10bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        var pomidoroBot = new PomidoroBot();
        telegramBotsApi.registerBot(pomidoroBot);
        new Thread(() -> {
            try {
                pomidoroBot.checkTimer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).run();   // запускаем поток. есть ещё похожий .start()
    }

    static class PomidoroBot extends TelegramLongPollingBot {

        @Override
        public String getBotUsername() {
            return "Pomidor TimerBot";
        }

        @Override
        public String getBotToken() {
            return "5756775825:AAH1m-iIDZLH9sfokiKDR1Q7XQ9NmVHncAU";
        }

        private static final ConcurrentHashMap<Timer, Long> timers = new ConcurrentHashMap<>();    // ConcurrentHashMap(структура данных) - позволяет работать в многопоточной среде

        enum TimerType {WORK, BREAK}

       static record Timer(Instant timer, TimerType timerType) { };

        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                var chatId = update.getMessage().getChatId();
                if (update.getMessage().getText().equals("/start")) { // equals - для сравнения содержимого объектов(не примитивных типов)

                    sendMsg(chatId.toString(), """
                            PomidoroBot Timer засекает время работы и отдыха в минутах.
                            Например '1 1'.
                            """);   // без toString() будет ошибка о несовместимости типов
                } else {
                    var args = update.getMessage().getText().split(" ");
                    if (args.length >= 1) {
                        // пользователь задаёт время работы и таймер остановится когда пройдёт это кол-во минут. сравнивается с системным временем.
                        var workTime = Instant.now().plus(Long.parseLong(args[0]), ChronoUnit.MINUTES); // workTime берём от текущего времени(Instant)
                        timers.put(new Timer(workTime, TimerType.WORK), chatId);

                        if (args.length >= 2) { // для второго значения(BREAK)
                            // пользователь задаёт время работы и таймер остановится когда пройдёт это кол-во минут. сравнивается с системным временем.
                            var breakTime = workTime.plus(Long.parseLong(args[1]), ChronoUnit.MINUTES); // breakTime = workTime + кол-во минут отдыха(второй аргумент)
                            timers.put(new Timer(workTime, TimerType.BREAK), chatId);
                        }
                    }
                }
            }
        }
            public void checkTimer () throws InterruptedException { // Метод. в бесконечном цикле пробегаемся и смотрим все таймеры. Запускается в отдельном потоке
                while (true) {
                    System.out.println("Количество таймеров пользователей " + timers.size());
                    timers.forEach((timer, userId) -> {
                        if (Instant.now().isAfter(timer.timer)) {   // если текущее время(Instant) находится после пользовательского таймера, значит время пользователя истекло
                            timers.remove(timer); // если время истекло - удаляем таймер из хранилища таймеров
                            switch (timer.timerType) {
                                case WORK -> sendMsg(userId.toString(), "Пора отдыхать");
                                case BREAK -> sendMsg(String.valueOf(userId), "Таймер завершён");
                            }
                        }
                    });
                    Thread.sleep(1000L); // 1 сек=1000 милисекунд.
                }
            }
        private void sendMsg(String chatId, String text) {
            SendMessage msg = new SendMessage(chatId, text);
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    } // static class PomidoroBot
}
