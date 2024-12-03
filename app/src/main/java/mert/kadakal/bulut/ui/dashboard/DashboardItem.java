package mert.kadakal.bulut.ui.dashboard;

public class DashboardItem {
    private final String link;
    private final String hesap;
    private final String tarih;
    private final long beğeni_sayısı;

    public DashboardItem(String link, String hesap, String tarih, long beğeniSayısı) {
        this.link = link;
        this.hesap = hesap;
        this.tarih = tarih;
        this.beğeni_sayısı = beğeniSayısı;
    }

    public String getLink() {
        return link;
    }

    public String getHesap() {
        return hesap;
    }

    public String getTarih() {
        return tarih;
    }

    public long getBeğeni_sayısı() {
        return beğeni_sayısı;
    }
}
