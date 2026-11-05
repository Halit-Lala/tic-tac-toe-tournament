package com.example.tictactoe.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.tictactoe.model.dto.response.TournamentUpdatedEvent;

@Component
public class TournamentBroadcaster {
    private final SimpMessagingTemplate messaging;

    public TournamentBroadcaster(
        SimpMessagingTemplate messaging
    ) {
        this.messaging = messaging;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTournamentUpdated(TournamentUpdatedEvent event) {
        if (event.tournament() != null) {
            messaging.convertAndSend("/topic/tournaments/" + event.tournament().id(), event.tournament());
        }
    }
}