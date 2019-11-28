import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Preconditions;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class SeleniumTest {

    private static RemoteWebDriver driver;
    private static WebDriverWait wait;

    @BeforeAll
    static void setup() {
        URL fileResource = SeleniumTest.class.getClassLoader().getResource("chromedriver");
        Preconditions.checkNotNull(fileResource, "Unable to initialize driver: File not found.");

        System.setProperty("webdriver.chrome.driver", fileResource.getFile());
    }

    @BeforeEach
    void launchBrowser() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);

        wait = new WebDriverWait(driver, 10);

        driver.navigate().to("https://stadium:goods2018!@stage.stadiumgoods.cloud/");

        wait.withMessage("Promo popup hasn't showed up.").until(driver -> {
            try {
                driver.findElement(By.cssSelector(".subscribe-modal-inner svg")).click();
                return true;
            } catch (NoSuchElementException | ElementNotVisibleException e) {
                return false;
            }
        });
    }

    @Test
    void verifyLogin() throws IOException, InvalidFormatException {
        File file = new File(
            Objects.requireNonNull(SeleniumTest.class.getClassLoader().getResource("test_data.xlsx")).getFile());

        Sheet sheet = WorkbookFactory.create(file).getSheet("Sheet1");

        String email;
        String password;

        List<String> errorMessages = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue;
            }

            driver.navigate().to("https://stage.stadiumgoods.cloud/customer/account/login/");

            email = row.getCell(0).getStringCellValue();
            driver.findElement(By.id("email")).sendKeys(email);

            password = row.getCell(1).getStringCellValue();
            driver.findElement(By.id("pass")).sendKeys(password);

            driver.findElement(By.id("send2")).click();

            try {
                wait.until(driver -> driver.getCurrentUrl().endsWith("customer/account/index/"));
            } catch (TimeoutException e) {
                errorMessages.add(String.format("%s:%s", email, password));
            }

            driver.manage().deleteAllCookies();
        }
        assertTrue(errorMessages.isEmpty(), String.format("Login failed for user(s): %s", ""));
    }

    @Test
    void sort() {
        List<WebElement> categories = driver.findElements(By.cssSelector("#header-nav li.level0"));

        assertEquals(10, categories.size(),
            "Unexpected number of categories in the page header.");

        int index = new Random().nextInt(categories.size() - 1);
        categories.get(index).click();

        List<Double> prices;
        try {
            driver.findElement(By.id("sort_by_chosen")).click();
            driver.findElement(By.cssSelector(".chosen-results li:nth-child(3)")).click();

            prices = driver.findElements(By.cssSelector("[itemprop=price]")).stream()
                .map(priceBox -> Double.parseDouble(priceBox.getAttribute("content")))
                .collect(Collectors.toList());

        } catch (NoSuchElementException e) {
            new Select(driver.findElement(By.cssSelector("label ~ select"))).selectByValue("PRICE_ASC");

            prices = driver.findElements(By.cssSelector("[class^=ProductSummary] ~ [class^=BodyText]")).stream()
                .map(priceBox -> Double.parseDouble(priceBox.getText().replaceAll("[\\$,]", "")))
                .collect(Collectors.toList());
        }

        List<Double> unsortedPrices = prices;
        Collections.sort(prices);

        assertEquals(unsortedPrices, prices, "Products are not sorted properly.");
    }

    @AfterEach
    void killBrowser() {
        driver.close();
        driver.quit();
    }
}
