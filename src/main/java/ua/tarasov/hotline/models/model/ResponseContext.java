package ua.tarasov.hotline.models.model;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;

import java.util.List;

@Getter
@Component
public class ResponseContext {
    private final List<SendMessage> answerMessages;
    private final List<EditMessageReplyMarkup> editMessageReplyMarkups;
    private final List<AnswerCallbackQuery> answerCallbackQueries;
    private final List<SendLocation> sendLocations;

    public ResponseContext(List<SendMessage> answerMessages, List<EditMessageReplyMarkup> editMessageReplyMarkups,
                           List<AnswerCallbackQuery> answerCallbackQueries, List<SendLocation> sendLocations) {
        this.answerMessages = answerMessages;
        this.editMessageReplyMarkups = editMessageReplyMarkups;
        this.answerCallbackQueries = answerCallbackQueries;
        this.sendLocations = sendLocations;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<SendMessage> answerMessages;
        private List<EditMessageReplyMarkup> editMessageReplyMarkups;
        private List<AnswerCallbackQuery> answerCallbackQueries;
        private List<SendLocation> sendLocations;

        public Builder sendMessage(List<SendMessage> sendMessages) {
            this.answerMessages = sendMessages;
            return this;
        }

        public Builder editMessageReplyMarkup(List<EditMessageReplyMarkup> editMessageReplyMarkup) {
            this.editMessageReplyMarkups = editMessageReplyMarkup;
            return this;
        }

        public Builder answerCallbackQuery(List<AnswerCallbackQuery> answerCallbackQuery) {
            this.answerCallbackQueries = answerCallbackQuery;
            return this;
        }

        public Builder sendLocation(List<SendLocation> sendLocations){
            this.sendLocations = sendLocations;
            return this;
        }

        public ResponseContext build() {
            return new ResponseContext(this.answerMessages, this.editMessageReplyMarkups, this.answerCallbackQueries, sendLocations);
        }
    }
}
