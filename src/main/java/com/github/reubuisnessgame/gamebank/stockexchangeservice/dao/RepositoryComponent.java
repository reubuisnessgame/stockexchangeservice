package com.github.reubuisnessgame.gamebank.stockexchangeservice.dao;

import com.github.reubuisnessgame.gamebank.stockexchangeservice.form.FullTeamForm;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.ShareModel;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.TeamModel;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.repository.ShareRepository;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.repository.TeamsRepository;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.repository.UserRepository;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import javassist.NotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class RepositoryComponent {

    private final TeamsRepository teamsRepository;

    private final
    JwtTokenProvider jwtTokenProvider;

    private final ShareRepository shareRepository;

    public RepositoryComponent(TeamsRepository teamsRepository, JwtTokenProvider jwtTokenProvider, ShareRepository shareRepository) {
        this.teamsRepository = teamsRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.shareRepository = shareRepository;
    }


    TeamModel getTeamByToken(String token) {
        Long userId = getUserIdFromToken(token);
        return teamsRepository.findById(userId).orElseThrow(() ->
                new UsernameNotFoundException("Team ID: " + userId + " not found"));
    }


    private Long getUserIdFromToken(String token) {
        Jws<Claims> claims = jwtTokenProvider.getClaims(resolveToken(token));
        return (Long) claims.getBody().get("userId");
    }


    TeamModel getTeamByNumber(Long number) {
        return teamsRepository.findByTeamNumber(number).orElseThrow(() ->
                new UsernameNotFoundException("Number: " + number + " not found"));
    }

    TeamModel saveTeam(TeamModel model) {
        return teamsRepository.save(model);
    }

    FullTeamForm getTeamFullInfo(TeamModel teamModel) {
        Iterable<ShareModel> shareModels = shareRepository.findAllByUserId(teamModel.getId());
        FullTeamForm fullTeamForm = new FullTeamForm();
        fullTeamForm.setTeam(teamModel);
        fullTeamForm.setShares(shareModels);
        return fullTeamForm;
    }


    private String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new IllegalArgumentException("Incorrect token");
    }


}
