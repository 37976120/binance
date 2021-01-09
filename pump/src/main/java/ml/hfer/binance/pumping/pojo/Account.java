package ml.hfer.binance.pumping.pojo;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Account {
    private ArrayList<CoinInfo> balances;
}
