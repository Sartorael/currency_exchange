package ru.skillbox.currency.exchange.entity;

import liquibase.pro.packaged.S;
import lombok.Data;

import javax.persistence.Column;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.List;

@Data
@XmlRootElement(name = "ValCurs")
@XmlAccessorType(XmlAccessType.FIELD)
public class CurrencyXML {

    @XmlElement(name = "Valute")
    private List<Valute> valutes;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Valute {
        @XmlAttribute(name = "ID")
        private String id;

        @XmlElement(name = "NumCode")
        private String numCode;

        @XmlElement(name = "CharCode")
        private String charCode;

        @XmlElement(name = "Nominal")
        private String nominal;

        @XmlElement(name = "Name")
        private String name;

        @XmlElement(name = "Value")
        private String value;

    }
}