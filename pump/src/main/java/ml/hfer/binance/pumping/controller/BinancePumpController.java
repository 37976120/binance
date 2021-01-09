package ml.hfer.binance.pumping.controller;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import ml.hfer.binance.pumping.constant.SideENUM;
import ml.hfer.binance.pumping.pojo.OrderBook;
import ml.hfer.binance.pumping.pojo.PumpReq;
import ml.hfer.binance.pumping.pojo.RetMsg;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/pump")
public class BinancePumpController {

    @Value("${api.key}")
    private String realApiKey = "none";

    @Value("${api.secret}")
    private String realApiSecret = "none";

    @Value(("${base.endpoint}"))
    private String baseEndpoint = "none";

    private String ip = "127.0.0.1";
    private int port = 1080;

    private String btcBalance = "0.01";


    /**
     * @param
     * @return
     */
    @RequestMapping("/start")
    @ResponseBody
    public String pump(PumpReq req) {
//        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

        String symbol = req.getBase() + req.getQuote();

//        List<Integer> precision = getPrecision(symbol);
        //real market price
        String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=5m&limit=2";
        String rs = proxyGet(url);
        JSONArray jsonArray = JSONUtil.parseArray(rs);
        List firstData = (List) jsonArray.get(0);
        String priceOfStart = (String) firstData.get(1);//开盘价
        String volume = (String) firstData.get(5);//成交量


        BigDecimal priceOfStripZeros = new BigDecimal(priceOfStart).stripTrailingZeros();
        BigDecimal buyPrice = priceOfStripZeros.multiply(new BigDecimal(req.getMulti())).setScale(priceOfStripZeros.scale(), BigDecimal.ROUND_HALF_UP);
        BigDecimal sellPrice1 = priceOfStripZeros.multiply(new BigDecimal("3")).setScale(priceOfStripZeros.scale(), BigDecimal.ROUND_HALF_UP);
        BigDecimal sellPrice2 = priceOfStripZeros.multiply(new BigDecimal("3.5")).setScale(priceOfStripZeros.scale(), BigDecimal.ROUND_HALF_UP);
        BigDecimal sellPrice3 = priceOfStripZeros.multiply(new BigDecimal("4")).setScale(priceOfStripZeros.scale(), BigDecimal.ROUND_HALF_UP);


        BigDecimal volumeOfStripZeros = new BigDecimal(volume).stripTrailingZeros();
        BigDecimal quantity = new BigDecimal(req.getBtcBalance()).divide(buyPrice, volumeOfStripZeros.scale(), BigDecimal.ROUND_DOWN);

        String newBuyOrderRs = newOrder(symbol, buyPrice, quantity, SideENUM.BUY, req.getIdPrefix() + "hfer_buy_01");
        System.out.println("【BUY-RESULT】" + newBuyOrderRs);
        //TODO 默认成功
//
//
//        String buyOrderRs = null;
//        boolean stop = false;
//        long critimes = 0;
//        boolean isBuyed = false;
//        while (!stop) {
//            buyOrderRs = newOrder(symbol, buyPrice, quantity, SideENUM.BUY, req.getIdPrefix() + "hfer_buy_01");
//            RetMsg retMsg = JSONUtil.toBean(buyOrderRs, RetMsg.class);
//            if (buyOrderRs.contains(req.getIdPrefix() + "hfer_buy_01")) {
//                isBuyed = true;
//                break;
//            }
//            if (retMsg.getCode() != 0) {
//                if (retMsg.getCode() != -2010) {
//                    break;
//                }
//                System.out.println("1.请求失败：" + retMsg);
//                if (retMsg.getCode() == 429) {
//                    return "失败请求已超限";
//                }
//            }
//            if (critimes++ > 560) {
//                return "最终失败";
//            }
//        }
//        System.out.println("买入" + symbol + ":\n响应:" + buyOrderRs);
        String sellRes1 = newOrder(symbol, sellPrice1, quantity.multiply(new BigDecimal("0.5")).setScale(volumeOfStripZeros.scale(), BigDecimal.ROUND_DOWN), SideENUM.SELL
                , req.getIdPrefix() + "hfer_sell_01");
        System.out.println("【SELL-RESULT1】:" + sellRes1);
        String sellRes2 = newOrder(symbol, sellPrice2, quantity.multiply(new BigDecimal("0.3").setScale(volumeOfStripZeros.scale(), BigDecimal.ROUND_DOWN)), SideENUM.SELL
                , req.getIdPrefix() + "hfer_sell_02");
        System.out.println("【SELL-RESULT2】:" + sellRes2);
        String sellRes3 = newOrder(symbol, sellPrice3, quantity.multiply(new BigDecimal("0.2").setScale(volumeOfStripZeros.scale(), BigDecimal.ROUND_DOWN)), SideENUM.SELL
                , req.getIdPrefix() + "hfer_sell_03");
        System.out.println("【SELL-RESULT3】:" + sellRes3);

        return "done";
    }

    private List<Integer> getPrecision(String symbol) {
        String url = "https://api.binance.com/api/v3/ticker/bookTicker?symbol=DLTBTC";
        String rs = proxyGet(url);
        OrderBook orderBook = JSONUtil.toBean(rs, OrderBook.class);
        return null;
    }


    String newOrder(String symbol, BigDecimal price, BigDecimal quantity, SideENUM side, String newClientOrderId) {
        //真实市场价
        //symbol,side,price,
        long timestamp = System.currentTimeMillis();
        byte[] apikeyOnBytes = realApiSecret.getBytes();
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, apikeyOnBytes);
        String endPoint = baseEndpoint + "/api/v3/order?";
        String uri = "symbol=" + symbol +
                "&side=" + side + "&type=LIMIT&timeInForce=GTC&" +
                "quantity=" + quantity + "&" +
                "price=" + price + "&" +
                "newClientOrderId=" + newClientOrderId + "&" +
                "newOrderRespType=ACK&" +
                "timestamp=" + timestamp;
        String allUrl = endPoint + uri + "&signature=" + hMac.digestHex(uri);
        String body = proxyPost(allUrl);
        return body;
    }

    String proxyGet(String url) {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        String body = HttpRequest.get(url).setProxy(proxy).execute().body();
        return body;
    }

    String proxyPost(String url) {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        String body = HttpRequest.post(url).header("X-MBX-APIKEY", realApiKey).setProxy(proxy).execute().body();
        return body;
    }

    String testGet(String url) {
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2,SSLv3");
        HttpGet httpGet = new HttpGet(url);
        HttpHost proxy = new HttpHost("127.0.0.1", 1080, "https");
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setProxy(proxy).build();


        httpGet.setConfig(defaultRequestConfig);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig).build();
//        try {
//            httpclient = HttpClients.custom().
//                    setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).
//                    setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
//                        public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
//                            return true;
//                        }
//                    }).build()).build();
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (KeyManagementException e) {
//            e.printStackTrace();
//        } catch (KeyStoreException e) {
//            e.printStackTrace();
//        }
        String rs = null;
        HttpResponse execute = null;
        try {
            execute = httpclient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HttpEntity entity = execute.getEntity();
        try {
            rs = EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(rs);

        //------------
//        HttpHost proxy = new HttpHost("127.0.0.1", 10808);
//        DefaultHttpClient httpclient = new DefaultHttpClient();
//        httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
//
//        HttpPost httpost = new HttpPost(url);
//        HttpGet httpGet = new HttpGet(myurl);
////        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
////        nvps.add(new BasicNameValuePair("param name", param));
////        try {
////            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.ISO_8859_1));
////        } catch (UnsupportedEncodingException e) {
////            e.printStackTrace();
////        }
//        HttpResponse response = null;
//        try {
//            response = httpclient.execute(httpGet);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        HttpEntity entity = response.getEntity();
//        String rs = null;
//        try {
//            rs = EntityUtils.toString(entity);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Request Handled?: " + response.getStatusLine());
//        try {
//            InputStream in = entity.getContent();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        httpclient.getConnectionManager().shutdown();

        return rs;
    }


    public static void main(String[] args) throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);

        SSLSocketFactory factory = (SSLSocketFactory) context.getSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket();

        String[] protocols = socket.getSupportedProtocols();

        System.out.println("Supported Protocols: " + protocols.length);
        for (int i = 0; i < protocols.length; i++) {
            System.out.println(" " + protocols[i]);
        }

        protocols = socket.getEnabledProtocols();

        System.out.println("Enabled Protocols: " + protocols.length);
        for (int i = 0; i < protocols.length; i++) {
            System.out.println(" " + protocols[i]);
        }

    }
}
