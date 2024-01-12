package ru.skillbox.currency.exchange.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.skillbox.currency.exchange.dto.CurrencyDto;
import ru.skillbox.currency.exchange.entity.Currency;
import ru.skillbox.currency.exchange.entity.CurrencyXML;
import ru.skillbox.currency.exchange.mapper.CurrencyMapper;
import ru.skillbox.currency.exchange.repository.CurrencyRepository;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class CurrencyService {
    private final CurrencyMapper mapper;
    private final CurrencyRepository repository;

    public CurrencyDto getById(Long id) {
        log.info("CurrencyService method getById executed");
        Currency currency = repository.findById(id).orElseThrow(() -> new RuntimeException("Currency " +
                "not found with id: " + id));
        return mapper.convertToDto(currency);
    }

    public Double convertValue(Long value, Long numCode) {
        log.info("CurrencyService method convertValue executed");
        Currency currency = repository.findByIsoNumCode(numCode);
        return value * currency.getValue();
    }

    public CurrencyDto create(CurrencyDto dto) {
        log.info("CurrencyService method create executed");
        return mapper.convertToDto(repository.save(mapper.convertToEntity(dto)));
    }

    public Map<String, List<Map<String, Object>>> getAllCurrencies() {
        List<Currency> currencyEntities = repository.findAll();
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        List<Map<String, Object>> currencies = currencyEntities.stream()
                .map(entity -> {
                    Map<String, Object> currencyMap = new HashMap<>();
                    currencyMap.put("name", entity.getName());
                    currencyMap.put("value", entity.getValue());
                    return currencyMap;
                })
                .collect(Collectors.toList());
        result.put("currencies", currencies);
        return result;
    }

    @Autowired
    private CurrencyRepository currencyRepository;

    @Scheduled(fixedDelay = 120000 * 30)
    private void updateCurrencies() {
        try {
            URL url = new URL("https://cbr.ru/scripts/XML_daily.asp");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/xml");
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                    "Cp1251"));
            String body = br.lines().collect(Collectors.joining());
            StringReader reader = new StringReader(body);
            JAXBContext context = JAXBContext.newInstance(CurrencyXML.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            CurrencyXML currencyXml = (CurrencyXML) unmarshaller.unmarshal(reader);
            List<CurrencyXML.Valute> valutes = currencyXml.getValutes();
            List<Currency> currencies = valutes.stream().map(valute -> {
                Currency existingCurrency = currencyRepository.findByIsoNumCode(Long.parseLong(valute.getNumCode()));
                if (existingCurrency != null) {
                    existingCurrency.setName(valute.getName());
                    existingCurrency.setNominal(Long.parseLong(valute.getNominal()));
                    existingCurrency.setValue(Double.valueOf(valute.getValue().replaceAll(",", ".")));
                    existingCurrency.setIsoNumCode(Long.parseLong(valute.getNumCode()));
                    existingCurrency.setCharCodeISO(valute.getCharCode());
                    return existingCurrency;
                } else {
                    Currency newCurrency = new Currency();
                    newCurrency.setName(valute.getName());
                    newCurrency.setNominal(Long.parseLong(valute.getNominal()));
                    newCurrency.setValue(Double.valueOf(valute.getValue().replaceAll(",", ".")));
                    newCurrency.setIsoNumCode(Long.parseLong(valute.getNumCode()));
                    newCurrency.setCharCodeISO(valute.getCharCode());
                    return newCurrency;
                }
            }).collect(Collectors.toList());
            currencyRepository.saveAll(currencies);
        } catch (Exception e) {
            log.error(e.toString());
        }
        log.info("Currencies rates are updated successfully");
    }
}
