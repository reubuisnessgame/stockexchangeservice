package com.github.reubuisnessgame.gamebank.stockexchangeservice.dao;

import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.ChangingPriceModel;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.CompanyModel;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.ShareModel;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.TeamModel;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.repository.ChangingPriceRepository;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.repository.CompanyRepository;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.repository.ShareRepository;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Component
public class StockExchangeDAO {

    private final ShareRepository shareRepository;

    private final CompanyRepository companyRepository;

    private final ChangingPriceRepository changingPriceRepository;

    private final RepositoryComponent repositoryComponent;

    private static final int STOCK_PRICE_CHANGE = 300_000; //5 minutes

    private static final int CHANGING_DELAY = 120_000; //2 minutes

    private static final String formatDate = "HH:mm:ss";
    static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatDate);

    private Random random = new Random();


    private final Logger LOGGER = LoggerFactory.getLogger(StockExchangeDAO.class.getSimpleName());
    private boolean isGameStarted;

    public StockExchangeDAO(ShareRepository shareRepository,
                            CompanyRepository companyRepository, ChangingPriceRepository changingPriceRepository, RepositoryComponent repositoryComponent) {
        this.shareRepository = shareRepository;
        this.companyRepository = companyRepository;
        this.changingPriceRepository = changingPriceRepository;
        ChangingRandomPriceThread priceThread = new ChangingRandomPriceThread();
        priceThread.start();
        this.repositoryComponent = repositoryComponent;
    }

    public CompanyModel createCompany(String companyName, Double sharePrice, long fullCount) {
        if (sharePrice <= 0 || fullCount <= 0) {
            throw new IllegalArgumentException("Illegal data in creating company");
        }
        return companyRepository.findByCompanyName(companyName).orElse(companyRepository.save(
                new CompanyModel(sharePrice, companyName, fullCount)));
    }

    public CompanyModel changeSharesCount(String companyName, long count) throws IllegalAccessException {
        if (isGameStarted) {
            CompanyModel companyModel = companyRepository.findByCompanyName(companyName).orElseThrow(() ->
                    new UsernameNotFoundException("Company with name " + companyName + " not found"));
            long freeCount = companyModel.getFreeCount();
            long fullCount = companyModel.getFullCount();
            if (count < 0 && -count > freeCount) {
                count = -freeCount;
            }
            companyModel.setFullCount(fullCount + count);
            return calculateNewSharePrice(count, companyModel);
        }
        throw new IllegalAccessException("The game has not started yet");
    }

    public void changeSharePrice(long companyId, double changingPrice) throws IllegalAccessException {
        LOGGER.info("Is game started " + isGameStarted);
        if (isGameStarted) {
            CompanyModel model = companyRepository.findById(companyId).orElseThrow(() ->
                    new UsernameNotFoundException("Company with ID " + companyId + " not found"));
            if (-changingPrice > model.getSharePrice()) {
                throw new IllegalArgumentException("Changing price can not be less then price");
            }
            LOGGER.info("Start changing price");
            Thread t = new Thread(new ChangingWorkerRunnable(model, changingPrice));
            t.start();
            return;
        }
        throw new IllegalAccessException("The game has not started yet");
    }

    public Iterable<ShareModel> buyShares(long number, int count, String companyName) throws IllegalAccessException {
        LOGGER.info("Buying shares");
        if (isGameStarted) {
            LOGGER.info("Game ok");
            TeamModel teamModel = repositoryComponent.getTeamByNumber(number);
            CompanyModel companyModel = companyRepository.findByCompanyName(companyName).orElseThrow(() ->
                    new UsernameNotFoundException("Company with name " + companyName + " not found"));
            double fullPrice = count * companyModel.getSharePrice();
            if (fullPrice > teamModel.getScore()) {
                LOGGER.info("Recalculate count");
                count = (int) (teamModel.getScore() / companyModel.getSharePrice());
                fullPrice = count * companyModel.getSharePrice();

            }
            LOGGER.info("Full price: " + fullPrice + ", team: " + teamModel.getUsername() + ", score: " + teamModel.getScore());
            if (count < 0) {
                count = 0;
                LOGGER.info("Incorrect count in buying");
            }
            calculateNewSharePrice(-count, companyModel);
            teamModel.setScore(teamModel.getScore() - fullPrice);
            repositoryComponent.saveTeam(teamModel);
            shareRepository.save(new ShareModel(teamModel.getId(), companyModel.getId(), count, companyModel));
            return shareRepository.findAllByUserId(teamModel.getId());
        }
        throw new IllegalAccessException("The game has not started yet");
    }

    private CompanyModel calculateNewSharePrice(long count, CompanyModel companyModel) {
        double lastPrice;
        lastPrice = recalculateNewPrice(companyModel, count);
        LOGGER.info("Second calculate Step: " + lastPrice);
        lastPrice = Math.round(lastPrice * 100000.0) / 100000.0;
        companyModel.setFreeCount(companyModel.getFreeCount() + count);
        companyModel.setSharePrice(lastPrice);
        changingPriceRepository.save(new ChangingPriceModel(companyModel.getId(), lastPrice, simpleDateFormat.format(new Date())));
        return companyRepository.save(companyModel);
    }

    public Iterable<ShareModel> sellShares(long number, int count, String companyName) throws IllegalAccessException {
        if (isGameStarted) {
            TeamModel teamModel = repositoryComponent.getTeamByNumber(number);
            CompanyModel companyModel = companyRepository.findByCompanyName(companyName).orElseThrow(() ->
                    new UsernameNotFoundException("Company with name " + companyName + " not found"));
            ShareModel shareModel = shareRepository.findByCompanyIdAndUserId(companyModel.getId(), teamModel.getId()).orElseThrow(()
                    -> new IllegalArgumentException("Not found shares"));
            if (count >= shareModel.getSharesNumbers()) {
                count = shareModel.getSharesNumbers();
                shareRepository.deleteByCompanyIdAndUserId(companyModel.getId(), teamModel.getId());
            } else {
                shareModel.setSharesNumbers(shareModel.getSharesNumbers() - count);
                shareRepository.save(shareModel);
            }
            calculateNewSharePrice(count, companyModel);
            return shareRepository.findAllByUserId(teamModel.getId());
        }
        throw new IllegalAccessException("The game has not started yet");
    }

    public Iterable<CompanyModel> getAllCompanies() throws NotFoundException {
        Iterable<CompanyModel> companyModels = companyRepository.findAll();
        if (!companyModels.iterator().hasNext()) {
            throw new NotFoundException("Companies not found");
        }
        return companyModels;
    }

    public Iterable<ShareModel> getAllShares() throws NotFoundException {
        Iterable<ShareModel> shareModels = shareRepository.findAll();
        if (!shareModels.iterator().hasNext()) {
            throw new NotFoundException("Companies not found");
        }
        return shareModels;
    }

    public void deleteCompany(String companyName) {
        CompanyModel companyModel = companyRepository.findByCompanyName(companyName).orElseThrow(() ->
                new UsernameNotFoundException("Company with name " + companyName + " not found"));
        Long id = companyModel.getId();
        changingPriceRepository.deleteAllByCompanyId(id);
        companyRepository.deleteById(id);
        shareRepository.deleteAllByCompanyId(id);
    }

    public Iterable<ChangingPriceModel> getChangingPrise(String companyName) throws NotFoundException {
        CompanyModel companyModel = companyRepository.findByCompanyName(companyName).orElseThrow(() ->
                new UsernameNotFoundException("Company with name " + companyName + " not found"));
        return getChangingPrice(companyModel.getId());
    }

    private Iterable<ChangingPriceModel> getChangingPrice(long companyId) throws NotFoundException {
        Iterable<ChangingPriceModel> changingPriceModels = changingPriceRepository.findAllByCompanyId(companyId);
        if (!changingPriceModels.iterator().hasNext()) {
            throw new NotFoundException("Changing price not found");
        }
        return changingPriceModels;
    }

    public void clearAll() {
        LOGGER.info("Cleaning repository");
        shareRepository.deleteAll();
        companyRepository.deleteAll();
    }

    public void stopStartGame(boolean gameStarted) {
        LOGGER.info("Is game started " + isGameStarted);
        isGameStarted = gameStarted;
    }

    private synchronized double recalculateNewPrice(CompanyModel model, long count){
        double stablePrice = model.getStablePrice();
        return stablePrice * (2 - ((double)model.getFreeCount()- count)/model.getFullCount());
    }

    private double recalculateNewPrice(CompanyModel model){
        return  recalculateNewPrice(model, 0);
    }

    private class ChangingRandomPriceThread extends Thread {
        public void run() {
            try {
                //noinspection InfiniteLoopStatement
                while (true) {
                    Thread.sleep(STOCK_PRICE_CHANGE);
                    if (isGameStarted) {
                        LOGGER.info("Random changing price");
                        Iterable<CompanyModel> companyModels = companyRepository.findAll();
                        List<CompanyModel> companyModelList = new ArrayList<>();
                        List<ChangingPriceModel> changingPriceModels = new ArrayList<>();
                        companyModels.forEach((company) -> {
                            double lastPrice = company.getStablePrice();
                            boolean sign = random.nextBoolean();
                            if (sign) {
                                lastPrice *= ((double) (random.nextInt(10) + 89) / 100);
                            } else {
                                lastPrice *= ((double) (random.nextInt(10) + 101) / 100);
                            }
                            LOGGER.info("Last price in company " + company.getCompanyName() + " : " + lastPrice);
                            lastPrice = Math.round(lastPrice * 100000.0) / 100000.0;
                            if (lastPrice < 0) {
                                lastPrice = -lastPrice;
                            }
                            String date = simpleDateFormat.format(new Date());

                            company.setStablePrice(lastPrice);
                            lastPrice = recalculateNewPrice(company);
                            changingPriceModels.add(new ChangingPriceModel(company.getId(), lastPrice, date));
                            company.setSharePrice(lastPrice);
                            companyModelList.add(company);
                        });
                        changingPriceRepository.saveAll(changingPriceModels);
                        companyRepository.saveAll(companyModelList);
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
    }

    private class ChangingWorkerRunnable implements Runnable {

        private final CompanyModel model;
        private final double changingPrice;

        ChangingWorkerRunnable(CompanyModel model, double changingPrice) {
            this.model = model;
            this.changingPrice = changingPrice;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(CHANGING_DELAY);
                LOGGER.info("Changing price of company " + model.getCompanyName());
                double tmpSharePrice = model.getStablePrice() + changingPrice;
                tmpSharePrice = Math.round(tmpSharePrice * 100000.0) / 100000.0;
                model.setStablePrice(tmpSharePrice);
                double newFullPrice = recalculateNewPrice(model);
                model.setSharePrice(newFullPrice);
                changingPriceRepository.save(new ChangingPriceModel(model.getId(), newFullPrice,
                        simpleDateFormat.format(new Date())));
                companyRepository.save(model);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage(), e);
            }

        }
    }
}
