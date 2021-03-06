package ml.hfer.binance.pumping.pojo;

import lombok.Data;

import java.util.List;

@Data
public class OrderBook {

    private String symbol;

    private String bidPrice;
    private String bidQty;

    private String askQty;
    private String askPrice;

    private List<List<String>> asks;

}
