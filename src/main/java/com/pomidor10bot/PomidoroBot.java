package com.pomidor10bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

public class PomidoroBot extends TelegramLongPollingBot {
    private static final ConcurrentHashMap<Timer, Long> timers = new ConcurrentHashMap<>();    // ConcurrentHashMap(структура данных) - позволяет работать в многопоточной среде

    private final TimerDao timerDao;
    public PomidoroBot(TimerDao timerDao) { this.timerDao = timerDao; }
    enum TimerType {WORK, BREAK}

    static record Timer(Instant timer, TimerType timerType) { };
        @Override
        public String getBotUsername() {  return "Pomidor TimerBot"; }

        @Override
        public String getBotToken() { return ""; }

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
                        timerDao.save(update.getMessage().getChatId(), TimerType.WORK.toString());

                        if (args.length >= 1) { // для второго значения(BREAK)
                            // пользователь задаёт время работы и таймер остановится когда пройдёт это кол-во минут. сравнивается с системным временем.
                            var breakTime = workTime.plus(Long.parseLong(args[1]), ChronoUnit.MINUTES); // breakTime = workTime + кол-во минут отдыха(второй аргумент)
                            timers.put(new Timer(breakTime, TimerType.BREAK), chatId);
                            timerDao.save(update.getMessage().getChatId(), TimerType.BREAK.toString());
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
    }


