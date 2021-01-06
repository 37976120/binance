package ml.hfer.binance.pumping.controller;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import ml.hfer.binance.pumping.constant.SideENUM;
import ml.hfer.binance.pumping.pojo.OrderBook;
import ml.hfer.binance.pumping.pojo.PumpReq;
import ml.hfer.binance.pumping.pojo.RetMsg;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import sun.security.krb5.internal.KdcErrException;
import sun.security.krb5.internal.crypto.CksumType;
import sun.security.krb5.internal.crypto.HmacSha1Aes256CksumType;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/pump")
public class PumpController {

    private String apiKey = "ZWxnK5njqIfRqyu6xy5rTTSVo1NPY2YBhpkoFe0ACvGfhmNhbgiLbUyVkjgkAC3M";
    private String realApiKey = "Lt4XxauFXZh3LUMh92eOqg8HlxXKKYCqvGeCJNEg9xoaRHPwps2jZ6HB1m29CVx2";

    private String apiSecret = "hKQYyOGfptgSo9tbs6FuVhPJpDFQwkXMO7MdW48BnWlTt2neiKJVNLFKyGAu4jEc";
    private String realApiSecret = "lacZn0wDCaIfNnTI5qeqLqn2AyGYeofdMfEMQbiOadU2aclbmsUa4Ke7JOdZ6bJo";

    /**
     * @param
     * @return
     */
    @RequestMapping("/start")
    @ResponseBody
    public String pump(PumpReq req) {

        String symbol = req.getSymbol() + "USDT";
        List<Integer> precision = getPrecision(symbol);
        //真实市场价
        String url = "https://api.binance.com/api/v3/klines?symbol=" + req.getSymbol() + "USDT&interval=15m&limit=3";
        String rs = proxyGet(url);
        JSONArray jsonArray = JSONUtil.parseArray(rs);
        List firstData = (List) jsonArray.get(0);
        String priceOfStart = (String) firstData.get(1);
        BigDecimal priceOfStartBig = new BigDecimal(priceOfStart).stripTrailingZeros();
        BigDecimal buyPrice = priceOfStartBig.multiply(new BigDecimal("1.3")).setScale(priceOfStartBig.scale(), BigDecimal.ROUND_HALF_UP);

        BigDecimal quantity = new BigDecimal("0.01669739");

        String newOrderRet = newOrder(symbol, buyPrice, new BigDecimal("10"), SideENUM.SELL);

        //下单结果判断
        boolean buyIs = newOrderRet.contains("my_order_id_buy");
        if (buyIs) {//买单成功
            BigDecimal sellPrice = priceOfStartBig.multiply(new BigDecimal("3")).setScale(priceOfStartBig.scale(), BigDecimal.ROUND_HALF_UP);
            String sellRes = newOrder(symbol, sellPrice, new BigDecimal("10"), SideENUM.SELL);
        }


        return "done";
    }

    private List<Integer> getPrecision(String symbol) {
        String url = "https://api.binance.com/api/v3/ticker/bookTicker?symbol=DLTBTC";
        String rs = proxyGet(url);
        OrderBook orderBook = JSONUtil.toBean(rs, OrderBook.class);
        return null;
    }


    String newOrder(String symbol, BigDecimal price, BigDecimal quantity, SideENUM side) {
        //真实市场价
        //symbol,side,price,
        long timestamp = System.currentTimeMillis();
        byte[] apikeyOnBytes = realApiSecret.getBytes();
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, apikeyOnBytes);
        String endPoint = "https://api.binance.com/api/v3/order?";
        String uri = "symbol=" + symbol +
                "&side=" + side + "&type=LIMIT&timeInForce=GTC&" +
                "quantity=" + quantity + "&" +
                "price=" + price + "&" +
                "newClientOrderId=my_order_id_buy&" +
                "newOrderRespType=ACK&" +
                "timestamp=" + timestamp;
        String allUrl = endPoint + uri + "&signature=" + hMac.digestHex(uri);
        String body = proxyPost(allUrl);
        return body;
    }


    String proxyGet(String url) {
        String ip = "127.0.0.1";
        int port = 1080;
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        String body = HttpRequest.get(url).setProxy(proxy).execute().body();
        return body;
    }

    String proxyPost(String url) {
        String ip = "127.0.0.1";
        int port = 1080;
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        String body = HttpRequest.post(url).header("X-MBX-APIKEY", realApiKey).setProxy(proxy).execute().body();
        return body;
    }
}
