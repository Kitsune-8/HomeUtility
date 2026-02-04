package com.example.homeutility;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

@Entity(tableName = "utility_records")
public class UtilityRecord {
    @PrimaryKey(autoGenerate = true)
    @Exclude
    private int id;

    @ColumnInfo(name = "user_id")
    @PropertyName("userId")
    private String userId;

    @ColumnInfo(name = "year_month", index = true)
    @PropertyName("yearMonth")
    private String yearMonth;

    @ColumnInfo(name = "reading_date")
    @PropertyName("readingDate")
    private String readingDate;

    @ColumnInfo(name = "electricity")
    @PropertyName("electricity")
    private double electricity = 0.0;

    @ColumnInfo(name = "hot_water")
    @PropertyName("hotWater")
    private double hotWater = 0.0;

    @ColumnInfo(name = "cold_water")
    @PropertyName("coldWater")
    private double coldWater = 0.0;

    @ColumnInfo(name = "gas")
    @PropertyName("gas")
    private double gas = 0.0;

    @ColumnInfo(name = "electricity_tariff")
    @PropertyName("electricityTariff")
    private double electricityTariff = 0.0;

    @ColumnInfo(name = "hot_water_tariff")
    @PropertyName("hotWaterTariff")
    private double hotWaterTariff = 0.0;

    @ColumnInfo(name = "cold_water_tariff")
    @PropertyName("coldWaterTariff")
    private double coldWaterTariff = 0.0;

    @ColumnInfo(name = "gas_tariff")
    @PropertyName("gasTariff")
    private double gasTariff = 0.0;

    // Конструктор по умолчанию
    public UtilityRecord() {}
    @Ignore
    public UtilityRecord(String userId, String yearMonth) {
        this.userId = userId;
        this.yearMonth = yearMonth;
    }

    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public String getReadingDate() {
        return readingDate;
    }

    public double getElectricity() {
        return electricity;
    }

    public double getHotWater() {
        return hotWater;
    }

    public double getColdWater() {
        return coldWater;
    }

    public double getGas() {
        return gas;
    }

    public double getElectricityTariff() {
        return electricityTariff;
    }

    public double getHotWaterTariff() {
        return hotWaterTariff;
    }

    public double getColdWaterTariff() {
        return coldWaterTariff;
    }

    public double getGasTariff() {
        return gasTariff;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public void setReadingDate(String readingDate) {
        this.readingDate = readingDate;
    }

    public void setElectricity(double electricity) {
        this.electricity = electricity;
    }

    public void setHotWater(double hotWater) {
        this.hotWater = hotWater;
    }

    public void setColdWater(double coldWater) {
        this.coldWater = coldWater;
    }

    public void setGas(double gas) {
        this.gas = gas;
    }

    public void setElectricityTariff(double electricityTariff) {
        this.electricityTariff = electricityTariff;
    }

    public void setHotWaterTariff(double hotWaterTariff) {
        this.hotWaterTariff = hotWaterTariff;
    }

    public void setColdWaterTariff(double coldWaterTariff) {
        this.coldWaterTariff = coldWaterTariff;
    }

    public void setGasTariff(double gasTariff) {
        this.gasTariff = gasTariff;
    }

    // Методы для расчётов
    @Ignore
    @Exclude
    public double getElectricityCost() {
        return electricity * electricityTariff;
    }

    @Ignore
    @Exclude
    public double getHotWaterCost() {
        return hotWater * hotWaterTariff;
    }

    @Ignore
    @Exclude
    public double getColdWaterCost() {
        return coldWater * coldWaterTariff;
    }

    @Ignore
    @Exclude
    public double getGasCost() {
        return gas * gasTariff;
    }

    @Ignore
    @Exclude
    public double getTotalCost() {
        return getElectricityCost() + getHotWaterCost() +
                getColdWaterCost() + getGasCost();
    }

    @Override
    public String toString() {
        return "UtilityRecord{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", yearMonth='" + yearMonth + '\'' +
                ", electricity=" + electricity +
                ", totalCost=" + getTotalCost() +
                '}';
    }
}