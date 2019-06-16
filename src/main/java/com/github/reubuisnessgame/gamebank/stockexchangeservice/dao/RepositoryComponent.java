package com.github.reubuisnessgame.gamebank.stockexchangeservice.dao;

import com.github.reubuisnessgame.gamebank.stockexchangeservice.form.FullTeamForm;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.ShareModel;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.TeamModel;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.repository.ShareRepository;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.repository.TeamsRepository;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.security.jwt.JwtTokenProvider;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class RepositoryComponent {

    private final TeamsRepository teamsRepository;

    private final ShareRepository shareRepository;

    public RepositoryComponent(TeamsRepository teamsRepository, ShareRepository shareRepository) {
        this.teamsRepository = teamsRepository;
        this.shareRepository = shareRepository;
    }

    TeamModel getTeamByNumber(Long number) {
        return teamsRepository.findByTeamNumber(number).orElseThrow(() ->
                new UsernameNotFoundException("Number: " + number + " not found"));
    }

    void saveTeam(TeamModel model) {
        teamsRepository.save(model);
    }

    FullTeamForm getTeamFullInfo(TeamModel teamModel) {
        Iterable<ShareModel> shareModels = shareRepository.findAllByUserId(teamModel.getId());
        FullTeamForm fullTeamForm = new FullTeamForm();
        fullTeamForm.setTeam(teamModel);
        fullTeamForm.setShares(shareModels);
        return fullTeamForm;
    }


}
