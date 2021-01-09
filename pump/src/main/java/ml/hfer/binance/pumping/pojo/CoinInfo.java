package ml.hfer.binance.pumping.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CoinInfo {
    private String asset;

    private BigDecimal free;

    private BigDecimal locked;
}
