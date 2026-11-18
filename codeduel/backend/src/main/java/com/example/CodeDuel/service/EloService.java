package com.example.CodeDuel.service;

import com.example.CodeDuel.entity.UserAccount;
import com.example.CodeDuel.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EloService {
    private final UserAccountRepository users;

    public EloService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional
    public EloResult applyResult(String userId, boolean won, int opponentElo) {
        UserAccount user = users.findById(userId).orElse(null);
        if (user == null) return null;

        int rating = user.getElo();
        double expected = 1.0 / (1.0 + Math.pow(10.0, (opponentElo - rating) / 400.0));
        int kFactor = rating >= 2000 ? 24 : rating >= 1600 ? 28 : 32;
        int delta = (int) Math.round(kFactor * ((won ? 1.0 : 0.0) - expected));
        delta = Math.max(won ? 8 : -24, Math.min(won ? 32 : -8, delta));
        if (!won && rating + delta < 100) delta = 100 - rating;

        user.setElo(Math.max(100, rating + delta));
        user.setMatchesPlayed(user.getMatchesPlayed() + 1);
        if (won) user.setWins(user.getWins() + 1);
        else user.setLosses(user.getLosses() + 1);
        users.save(user);
        return new EloResult(user.getElo(), delta, getRank(user.getElo()));
    }

    public int getElo(String userId) {
        return users.findById(userId).map(UserAccount::getElo).orElse(1250);
    }

    public static String getRank(int elo) {
        if (elo >= 2200) return "Grandmaster";
        if (elo >= 1800) return "Diamond";
        if (elo >= 1500) return "Gold";
        if (elo >= 1250) return "Silver";
        return "Bronze";
    }

    public record EloResult(int elo, int delta, String rank) {}
}
