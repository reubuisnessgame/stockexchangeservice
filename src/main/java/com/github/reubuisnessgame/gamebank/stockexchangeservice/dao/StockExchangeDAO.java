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
import java.util.Date;
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
        return companyRepository.findByCompanyName(companyName).orElse(companyRepository.save(
                new CompanyModel(sharePrice, companyName, fullCount)));
    }

    public CompanyModel changeSharesCount(String companyName, long count) throws IllegalAccessException {
        if (isGameStarted) {
            CompanyModel companyModel = companyRepository.findByCompanyName(companyName).orElseThrow(() ->
                    new UsernameNotFoundException("Company with name " + companyName + "not found"));
            long freeCount = companyModel.getFreeCount();
            if (count < 0 && -count > freeCount) {
                count = freeCount;
            }
            return calculateNewSharePrice(count, companyModel);
        }
        throw new IllegalAccessException("The game has not started yet");
    }

    public void changeSharePrice(long companyId, double changingPrice) throws IllegalAccessException {
        if (isGameStarted) {
            Thread t = new Thread(new ChangingWorkerRunnable(companyId, changingPrice));
            t.start();
        }
        throw new IllegalAccessException("The game has not started yet");
    }

    public Iterable<ShareModel> buyShares(long number, int count, String companyName) throws IllegalAccessException {
        if (isGameStarted) {
            TeamModel teamModel = repositoryComponent.getTeamByNumber(number);
            CompanyModel companyModel = companyRepository.findByCompanyName(companyName).orElseThrow(() ->
                    new UsernameNotFoundException("Company with name " + companyName + "not found"));
            double fullPrice = count * companyModel.getSharePrice();
            if (fullPrice < teamModel.getScore()) {
                count = (int) (teamModel.getScore() / companyModel.getSharePrice());
                fullPrice = count * companyModel.getSharePrice();
            }
            calculateNewSharePrice(count, companyModel);
            teamModel.setScore(teamModel.getScore() - fullPrice);
            repositoryComponent.saveTeam(teamModel);
            shareRepository.save(new ShareModel(teamModel.getId(), companyModel.getId(), count, companyModel));
            return shareRepository.findAllByUserId(teamModel.getId());
        }
        throw new IllegalAccessException("The game has not started yet");
    }

    private CompanyModel calculateNewSharePrice(long count, CompanyModel companyModel) {
        double lastPrice = companyModel.getSharePrice();
        lastPrice /= 1 + companyModel.getFreeCount() / companyModel.getFullCount();
        lastPrice *= 1 + (companyModel.getFreeCount() + count) / companyModel.getFullCount();
        lastPrice = Math.round(lastPrice * 100000.0)/ 100000.0;
        companyModel.setFreeCount(companyModel.getFreeCount() + count);
        companyModel.setSharePrice(lastPrice);
        changingPriceRepository.save(new ChangingPriceModel(companyModel.getId(), lastPrice, simpleDateFormat.format(new Date())));
        return companyRepository.save(companyModel);
    }

    public Iterable<ShareModel> sellShares(long number, int count, String companyName) throws IllegalAccessException {
        if(isGameStarted) {
            TeamModel teamModel = repositoryComponent.getTeamByNumber(number);
            CompanyModel companyModel = companyRepository.findByCompanyName(companyName).orElseThrow(() ->
                    new UsernameNotFoundException("Company with name " + companyName + "not found"));
            ShareModel shareModel = shareRepository.findByCompanyIdAndUserId(companyModel.getId(), teamModel.getId()).orElseThrow(()
                    -> new IllegalArgumentException("Not found shares"));
            if (count >= shareModel.getSharesNumbers()) {
                count = shareModel.getSharesNumbers();
                shareRepository.deleteByCompanyIdAndUserId(companyModel.getId(), teamModel.getId());
            } else {
                shareModel.setSharesNumbers(shareModel.getSharesNumbers() - count);
                shareRepository.save(shareModel);
            }
            calculateNewSharePrice(-count, companyModel);
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
                new UsernameNotFoundException("Company with name " + companyName + "not found"));
        Long id = companyModel.getId();
        changingPriceRepository.deleteAllByCompanyId(id);
        companyRepository.deleteById(id);
        shareRepository.deleteAllByCompanyId(id);
    }

    public Iterable<ChangingPriceModel> getChangingPrise(String companyName) throws NotFoundException {
        CompanyModel companyModel = companyRepository.findByCompanyName(companyName).orElseThrow(() ->
                new UsernameNotFoundException("Company with name " + companyName + "not found"));
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
        shareRepository.deleteAll();
        companyRepository.deleteAll();
    }

    public void stopStartGame(boolean gameStarted) {
        isGameStarted = gameStarted;
    }

    class ChangingRandomPriceThread extends Thread {
        public void run() {
            try {
                //noinspection InfiniteLoopStatement
                while (true) {
                    Thread.sleep(STOCK_PRICE_CHANGE);
                    if (isGameStarted) {
                        Iterable<CompanyModel> companyModels = companyRepository.findAll();
                        companyModels.forEach((company) -> {
                            double lastPrice = company.getSharePrice();
                            boolean sign = random.nextBoolean();
                            if (sign) {
                                lastPrice *= ((double) (random.nextInt(10) + 90) / 100);
                            } else {
                                lastPrice *= ((double) (random.nextInt(10) + 10) / 100);
                            }
                            String date = simpleDateFormat.format(new Date());
                            changingPriceRepository.save(new ChangingPriceModel(company.getId(), lastPrice, date));
                            company.setSharePrice(lastPrice);
                        });
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
    }

    class ChangingWorkerRunnable implements Runnable {

        private final long companyId;
        private final double changingPrice;

        ChangingWorkerRunnable(long companyId, double changingPrice) {
            this.companyId = companyId;
            this.changingPrice = changingPrice;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(CHANGING_DELAY);
                CompanyModel companyModel = companyRepository.findById(companyId).orElseThrow(() ->
                        new UsernameNotFoundException("Company with ID " + companyId + "not found"));
                double tmpSharePrice = companyModel.getSharePrice() + changingPrice;
                companyModel.setSharePrice(tmpSharePrice);
                changingPriceRepository.save(new ChangingPriceModel(companyModel.getId(), tmpSharePrice,
                        simpleDateFormat.format(new Date())));
                companyRepository.save(companyModel);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage(), e);
            }

        }
    }
}
