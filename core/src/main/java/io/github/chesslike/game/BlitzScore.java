package io.github.chesslike.game;

import java.util.EnumSet;
import java.util.Set;

public class BlitzScore {
    public static class Result {
        public final int points;
        public final int basePoints;
        public final int multiplier;
        public final String label;
        public final boolean multiplierUp;
        public Result(int points, int basePoints, int multiplier, String label, boolean multiplierUp) {
            this.points=points; this.basePoints=basePoints; this.multiplier=multiplier;
            this.label=label; this.multiplierUp=multiplierUp;
        }
    }
    private int score, streak, multiplier, bestStreak, permanentMultiplierBonus, permanentBaseBonus;
    private int roomCaptures, totalCaptures, hungryBonusPercent, cardsPlayed;
    private final Set<BlitzRelic> relics=EnumSet.noneOf(BlitzRelic.class);
    public BlitzScore(){reset();}
    public void reset(){score=0;streak=0;multiplier=1;bestStreak=0;permanentMultiplierBonus=0;permanentBaseBonus=0;roomCaptures=0;totalCaptures=0;hungryBonusPercent=0;cardsPlayed=0;}
    public void setRelics(Iterable<BlitzRelic> activeRelics){relics.clear();if(activeRelics!=null)for(BlitzRelic r:activeRelics)if(r!=null)relics.add(r);}
    private boolean has(BlitzRelic r){return relics.contains(r);}

    public Result addCapture(boolean cursed,String movement,boolean assassination,int enemyHealthPercent,int gold,int room,float timeRemaining){
        totalCaptures++; roomCaptures++; streak++; bestStreak=Math.max(bestStreak,streak);
        int oldMultiplier=multiplier; updateMultiplier();
        int base=100+permanentBaseBonus;
        if(has(BlitzRelic.ENDLESS_FANG)&&totalCaptures%10==0){permanentBaseBonus+=25;base+=25;}
        if(has(BlitzRelic.BLOODIED_CROWN))base+=Math.round(base*Math.min(100,streak*5)/100f);
        if(has(BlitzRelic.HUNGRY_BLADE))base+=Math.round(base*hungryBonusPercent/100f);
        if(has(BlitzRelic.GOLD_CLOCK))base+=(Math.max(0,gold)/50)*25;
        if(has(BlitzRelic.GOLDEN_MOMENT))base+=(Math.max(0,gold)/100)*100;

        int percent=0; String label="CAPTURE";
        if(has(BlitzRelic.BLACK_KNIGHT)&&"KNIGHT".equals(movement))percent+=100;
        if(has(BlitzRelic.ROYAL_SEAL)&&("QUEEN".equals(movement)||"ROOK".equals(movement)||"BISHOP".equals(movement)))percent+=75;
        if(has(BlitzRelic.EXECUTIONERS_EYE)&&enemyHealthPercent>0&&enemyHealthPercent<50)percent+=50;
        if(has(BlitzRelic.ASSASSINS_VEIL)&&assassination){percent+=100;label="ASSASSINATION";}
        if(has(BlitzRelic.VOID_MIRROR)&&assassination)percent+=50;
        if(has(BlitzRelic.COMBO_ENGINE)&&streak>=3)percent+=25;
        if(has(BlitzRelic.OVERDRIVE)&&multiplier>=4)percent+=50;
        if(has(BlitzRelic.MERCILESS_EDGE)&&multiplier>=3)percent+=30;
        if(has(BlitzRelic.BROKEN_CROWN)&&multiplier>=5)percent+=25;
        if(has(BlitzRelic.LAST_STAND)&&timeRemaining<=15f)percent+=100;
        if(cursed&&has(BlitzRelic.CURSED_COIN)){base+=300;label="CURSED EXECUTION";}
        if(cursed&&has(BlitzRelic.WAR_DRUM))base+=150;

        int pre=base+Math.round(base*percent/100f);
        pre+=Math.max(0,gold)/100*(has(BlitzRelic.GOLDEN_FANG)?50:0);
        if(has(BlitzRelic.GLASS_DAGGER)&&roomCaptures==1){pre+=500;label="FIRST BLOOD";}
        if(has(BlitzRelic.CHAIN_LINK)&&totalCaptures%4==0){pre+=300;label="CHAIN LINK";}
        if(has(BlitzRelic.CRITICAL_MASS)&&totalCaptures%6==0){pre+=500;label="CRITICAL MASS";}
        int points=pre*Math.max(1,multiplier);
        if(has(BlitzRelic.ECHO_STONE)&&totalCaptures%3==0){points+=Math.round(points*.5f);label="ECHO CHAIN";}
        score+=points;
        if(has(BlitzRelic.HUNGRY_BLADE))hungryBonusPercent=Math.min(500,hungryBonusPercent+10);
        return new Result(points,pre,multiplier,label,multiplier>oldMultiplier);
    }
    public Result addCapture(boolean cursed,String movement,boolean assassination,int enemyHealthPercent,int gold,int room){return addCapture(cursed,movement,assassination,enemyHealthPercent,gold,room,999f);}
    public int addCapture(){return addCapture(false,"",false,100,0,0,999f).points;}
    public int addSpecialCapture(int bonus){streak++;bestStreak=Math.max(bestStreak,streak);updateMultiplier();int p=(100+bonus)*multiplier;score+=p;return p;}
    public int addRoomClear(){int p=500*multiplier;if(has(BlitzRelic.HASTE_CORE))p+=250;score+=p;return p;}
    public int addSpell(){int p=75*multiplier;score+=p;return p;}
    public int addCardPlay(){cardsPlayed++;int p=25*multiplier;score+=p;return p;}
    public void breakStreak(){streak=0;multiplier=1+permanentMultiplierBonus;hungryBonusPercent=0;}
    private void updateMultiplier(){
        if(streak>=15)multiplier=5;else if(streak>=10)multiplier=4;else if(streak>=6)multiplier=3;else if(streak>=3)multiplier=2;else multiplier=1;
        multiplier+=permanentMultiplierBonus;
        if(has(BlitzRelic.MOMENTUM_CORE)&&streak%5==0){permanentMultiplierBonus++;multiplier++;}
    }
    public int getScore(){return score;} public int getStreak(){return streak;} public int getMultiplier(){return multiplier;}
    public int getBestStreak(){return bestStreak;} public int getTotalCaptures(){return totalCaptures;}
    public int getRoomCaptures(){return roomCaptures;} public int getCardsPlayed(){return cardsPlayed;} public void addBonusScore(int points){score+=Math.max(0,points);}
}
