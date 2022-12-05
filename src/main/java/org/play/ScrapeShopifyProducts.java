package org.play;

import com.google.gson.Gson;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ScrapeShopifyProducts {
    private static void log(String output) {
        System.out.println(output);
    }

    private static List<String> allCollectionURLs = new ArrayList<>();

    private static final List<String> allProductURLs = new ArrayList<>();
    private static String getRow(Document productDoc) {
        StringBuilder row = new StringBuilder();

        String nameText = productDoc.title().substring(0, productDoc.title().indexOf('–'));
        Elements names = productDoc.select(".nt_name_current");
        for (Element name : names) {
//                skuText = name.text();
            nameText = nameText + name.text();
//                log("\n\t" + name.text());
        }

        row.append(nameText);
        row.append(",");

        Elements skus = productDoc.select("#pr_sku_ppr");
        String skuText = "";
        for (Element sku : skus) {
            skuText = sku.text();
//                log("SKU: " + skuText);
            row.append(skuText);
            row.append(",");

        }

        Elements images = productDoc.select(".p_ptw");

        String imageURL;
        for (Element image : images) {
            imageURL = image.absUrl("data-src");
            if (imageURL.contains(skuText)) {
                row.append(imageURL);
                row.append(",");

//                    log("images: " + imageURL);
            }
        }

        String rowOutput = row.toString();
        return rowOutput.substring(0, rowOutput.length() - 1);
    }

    private static void getProductDetails(String productURL) {
        try {
            InputStream input = new URL(productURL).openStream();
            Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            Product data = new Gson().fromJson(reader, Product.class);

            for (ProductVariant variant : data.variants) {
                StringBuilder row = new StringBuilder();
                row.append(variant.name.replaceAll("- ", ""));
                row.append(",");
                row.append(variant.sku);
                row.append(",");
                boolean oneImageFound = false;
                for (String imageURL : data.images) {
                    if (imageURL.contains(variant.sku)) {
                        row.append(imageURL.replaceAll("//", "https://"));
                        row.append(",");
                        oneImageFound = true;
                    }
                }

                if (!oneImageFound) {
                    for (String imageURL : data.images) {
                        row.append(imageURL.replaceAll("//", "https://"));
                        row.append(",");
                    }
                }

                String rowOutput = row.toString();
                log(rowOutput.substring(0, rowOutput.length() - 1));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static void getAllProductURLs() {
        // get collection urls
        try {
            for (String collectionURL : allCollectionURLs) {
                Document doc = Jsoup.connect(collectionURL).get();
                log(doc.title());
                Elements productlinks = doc.select(".products a");
                for (Element productlink : productlinks) {
                    if (!allProductURLs.contains(productlink.absUrl("href") + ".js")) {
                        log("%s\n\t%s" + productlink.absUrl("href") + ".js");
                        allProductURLs.add(productlink.absUrl("href") + ".js");
                        try {
                            TimeUnit.MILLISECONDS.sleep(200);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

        log(" *** Get All Collections *** \n\n");

        getConfiguredCollectionsURLS();

        log(" *** Get All Products *** \n\n");

        getAllProductURLs();

        log(" *** Display product scrapping *** \n\n");

        log("name,sku,image(s)");

        for (String productURL : allProductURLs) {

            getProductDetails(productURL);
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void getConfiguredCollectionsURLS() {
        try {
            PropertiesHelper reader = new PropertiesHelper("application.properties");
            String property = reader.getProperty("allCollectionURLs");
            allCollectionURLs.addAll(Arrays.asList(property.split(",")));

            log("All Collection URLs to scrape : " + allCollectionURLs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
