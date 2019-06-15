package com.github.reubuisnessgame.gamebank.stockexchangeservice.controller;

import com.github.reubuisnessgame.gamebank.stockexchangeservice.dao.NewsDAO;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.dao.StockExchangeDAO;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.form.NewNewsForm;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.form.StartGameForm;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.ExceptionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/stock")
public class StockExchangeController {
    private final StockExchangeDAO stockExchangeDAO;

    private final NewsDAO newsDAO;

    public StockExchangeController(StockExchangeDAO stockExchangeDAO, NewsDAO newsDAO) {
        this.stockExchangeDAO = stockExchangeDAO;
        this.newsDAO = newsDAO;
    }


    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('EXCHANGE_WORKER')")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public ResponseEntity createCompany(@RequestParam(value = "name") String companyName,
                                        @RequestParam(value = "price") double sharePrice,
                                        @RequestParam(value = "count") long fullCount) {
        try {
            return ResponseEntity.ok(stockExchangeDAO.createCompany(companyName, sharePrice, fullCount));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", e.getMessage(), "/stock/create"));
        }
    }

    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('EXCHANGE_WORKER')")
    @RequestMapping(value = "/change", method = RequestMethod.POST)
    public ResponseEntity changeSharePrice(@RequestBody NewNewsForm form) {
        try {
            stockExchangeDAO.changeSharePrice(form.getCompanyId(), form.getChangingPrice());
            return ResponseEntity.ok(newsDAO.createNews(form.getHeading(), form.getArticle()));
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(403).body(new ExceptionModel(403, "Forbidden", e.getMessage(), "/team/rpl_credit"));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new ExceptionModel(400, "Bad Request", e.getMessage(), "/stock/change"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", e.getMessage(), "/stock/change"));
        }
    }

    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('EXCHANGE_WORKER')")
    @RequestMapping(value = "/change_count", method = RequestMethod.POST)
    public ResponseEntity changeSharesCount(@RequestParam(value = "name") String companyName,
                                            @RequestParam(value = "count") long count) {
        try {
            return ResponseEntity.ok(stockExchangeDAO.changeSharesCount(companyName, count));
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(403).body(new ExceptionModel(403, "Forbidden", e.getMessage(), "/team/rpl_credit"));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new ExceptionModel(400, "Bad Request", e.getMessage(), "/stock/change_count"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", e.getMessage(), "/stock/change_count"));
        }
    }

    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('EXCHANGE_WORKER')")
    @RequestMapping(value = "/buy/{number}", method = RequestMethod.POST)
    public ResponseEntity buyShares(@PathVariable Long number, @RequestParam int count, @RequestParam String companyName) {
        try {
            return ResponseEntity.ok(stockExchangeDAO.buyShares(number, count, companyName));
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(403).body(new ExceptionModel(403, "Forbidden", e.getMessage(), "/team/rpl_credit"));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new ExceptionModel(400, "Bad Request", e.getMessage(), "/stock/buy" + number));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", e.getMessage(), "/stock/buy/" + number));
        }
    }

    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('EXCHANGE_WORKER')")
    @RequestMapping(value = "/sell/{number}", method = RequestMethod.POST)
    public ResponseEntity sellShares(@PathVariable Long number, @RequestParam int count, @RequestParam String companyName) {
        try {
            return ResponseEntity.ok(stockExchangeDAO.sellShares(number, count, companyName));
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(403).body(new ExceptionModel(403, "Forbidden", e.getMessage(), "/team/rpl_credit"));
        } catch (UsernameNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ExceptionModel(400, "Bad Request", e.getMessage(), "/stock/sell/" + number));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", e.getMessage(), "/stock/sell/" + number));
        }
    }

    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('EXCHANGE_WORKER') or hasAuthority('TEAM')")
    @RequestMapping(value = "/changes/{companyName}", method = RequestMethod.GET)
    public ResponseEntity getChangingPrice(@PathVariable String companyName) {
        try {
            return ResponseEntity.ok(stockExchangeDAO.getChangingPrise(companyName));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new ExceptionModel(400, "Bad Request", e.getMessage(), "/stock/changes/" + companyName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", e.getMessage(), "/stock/changes/" + companyName));
        }
    }

    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('EXCHANGE_WORKER') or hasAuthority('TEAM')")
    @RequestMapping(value = "/companies}", method = RequestMethod.GET)
    public ResponseEntity getAllCompanies() {
        try {
            return ResponseEntity.ok(stockExchangeDAO.getAllCompanies());
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new ExceptionModel(400, "Bad Request", e.getMessage(), "/stock/companies"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", e.getMessage(), "/stock/companies"));
        }
    }

    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('EXCHANGE_WORKER')")
    @RequestMapping(value = "/shares}", method = RequestMethod.GET)
    public ResponseEntity getAllShares() {
        try {
            return ResponseEntity.ok(stockExchangeDAO.getAllShares());
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new ExceptionModel(400, "Bad Request", e.getMessage(), "/stock/companies"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", e.getMessage(), "/stock/companies"));
        }
    }

    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('EXCHANGE_WORKER') or hasAuthority('TEAM')")
    @RequestMapping(value = "/news}", method = RequestMethod.GET)
    public ResponseEntity getAllNews() {
        try {
            return ResponseEntity.ok(newsDAO.getAllNews());
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new ExceptionModel(400, "Bad Request", e.getMessage(), "/stock/companies"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", e.getMessage(), "/stock/companies"));
        }
    }

    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('EXCHANGE_WORKER')")
    @RequestMapping(value = "/delete/{companyName}", method = RequestMethod.POST)
    public ResponseEntity deleteCompany(@PathVariable String companyName) {
        try {
            stockExchangeDAO.deleteCompany(companyName);
            return ResponseEntity.ok().build();
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new ExceptionModel(400, "Bad Request", e.getMessage(), "/stock/changes/" + companyName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", e.getMessage(), "/stock/changes/" + companyName));
        }
    }

    @PreAuthorize("hasAuthority('MODERATOR')")
    @RequestMapping(value = "/game", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity startGame(@RequestBody StartGameForm form) {
        try {
            stockExchangeDAO.stopStartGame(form.isStated());
            return ResponseEntity.ok().build();
        } catch (Throwable t) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", t.getMessage(), "/admin/start"));
        }
    }


    @PreAuthorize("hasAuthority('MODERATOR')")
    @RequestMapping(value = "/clear", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity clearAll() {
        try {
            stockExchangeDAO.clearAll();
            newsDAO.clearAll();
            return ResponseEntity.ok().build();
        } catch (Throwable t) {
            return ResponseEntity.status(500).body(new ExceptionModel(500, "Internal Error", t.getMessage(), "/admin/start"));
        }
    }


}
