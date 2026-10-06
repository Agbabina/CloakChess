package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Align;

public class BlitzRelicShopScreen {
    public static final int NONE=-2, LEAVE=-1;
    private final SpriteBatch batch; private final ShapeRenderer shapeRenderer; private final BitmapFont font;
    private final Array<BlitzRelic> offers=new Array<>(); private final Array<Rectangle> rects=new Array<>();
    private final Rectangle panel=new Rectangle(),leaveRect=new Rectangle(); private String message="";
    public BlitzRelicShopScreen(SpriteBatch b,ShapeRenderer s,BitmapFont f){batch=b;shapeRenderer=s;font=f;}
    public void show(Iterable<BlitzRelic> newOffers){offers.clear();if(newOffers!=null)for(BlitzRelic r:newOffers)if(r!=null)offers.add(r);message="";}
    public void hide(){offers.clear();rects.clear();message="";}
    public boolean isVisible(){return !offers.isEmpty();}
    public BlitzRelic getOffer(int i){return i>=0&&i<offers.size?offers.get(i):null;}
    public void setMessage(String m){message=m==null?"":m;}
    private void layout(){float w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight(),pw=Math.min(w-28,900),ph=Math.min(h-28,540);panel.set((w-pw)/2,(h-ph)/2,pw,ph);rects.clear();float gap=14,bw=(pw-72-gap)/2,bh=Math.min(170,(ph-155-gap)/2),top=panel.y+ph-78;for(int i=0;i<offers.size;i++){int c=i%2,r=i/2;rects.add(new Rectangle(panel.x+30+c*(bw+gap),top-(r+1)*bh-r*gap,bw,bh));}leaveRect.set(panel.x+pw-155,panel.y+20,125,42);}
    public int handleClick(float x,float y){if(!isVisible())return NONE;layout();if(leaveRect.contains(x,y))return LEAVE;for(int i=0;i<rects.size;i++)if(rects.get(i).contains(x,y))return i;return NONE;}
    public void render(int gold,BlitzManager manager){if(!isVisible())return;layout();float w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);shapeRenderer.setColor(0,0,0,.86f);shapeRenderer.rect(0,0,w,h);
        shapeRenderer.setColor(.55f,.38f,.12f,1);shapeRenderer.rect(panel.x-3,panel.y-3,panel.width+6,panel.height+6);
        shapeRenderer.setColor(.07f,.055f,.10f,1);shapeRenderer.rect(panel.x,panel.y,panel.width,panel.height);
        for(int i=0;i<offers.size;i++){BlitzRelic r=offers.get(i);Rectangle box=rects.get(i);boolean owned=manager.hasRelic(r),ok=gold>=manager.getRelicCost(r);shapeRenderer.setColor(owned?new Color(.12f,.12f,.14f,1):ok?new Color(.17f,.20f,.29f,1):new Color(.20f,.13f,.16f,1));shapeRenderer.rect(box.x,box.y,box.width,box.height);shapeRenderer.setColor(owned?new Color(.25f,.25f,.28f,1):new Color(1,.70f,.20f,1));shapeRenderer.rect(box.x,box.y+box.height-3,box.width,3);}
        shapeRenderer.setColor(.18f,.34f,.25f,1);shapeRenderer.rect(leaveRect.x,leaveRect.y,leaveRect.width,leaveRect.height);shapeRenderer.end();
        batch.begin();font.getData().setScale(1.25f);font.setColor(new Color(1,.78f,.25f,1));font.draw(batch,"BLITZ RELIC FORGE",panel.x+28,panel.y+panel.height-28);
        font.getData().setScale(.65f);font.setColor(Color.WHITE);font.draw(batch,"GOLD: "+gold+"     TIME: "+(int)Math.ceil(manager.getTimeRemaining())+"s",panel.x+panel.width-310,panel.y+panel.height-28);
        for(int i=0;i<offers.size;i++){BlitzRelic r=offers.get(i);Rectangle box=rects.get(i);boolean owned=manager.hasRelic(r);font.getData().setScale(.72f);font.setColor(owned?Color.GRAY:Color.WHITE);font.draw(batch,r.getName(),box.x+12,box.y+box.height-14);
            font.getData().setScale(.50f);font.setColor(new Color(.76f,.79f,.88f,1));font.draw(batch,r.getDescription(),box.x+12,box.y+box.height-42,box.width-24,Align.left,true);
            font.getData().setScale(.65f);font.setColor(owned?Color.GRAY:new Color(1,.78f,.25f,1));font.draw(batch,owned?"ACTIVE":"COST: "+manager.getRelicCost(r),box.x+12,box.y+24);}
        if(!message.isEmpty()){font.getData().setScale(.62f);font.setColor(new Color(.35f,.85f,1,1));font.draw(batch,message,panel.x+28,panel.y+30);}
        font.getData().setScale(.72f);font.setColor(Color.WHITE);font.draw(batch,"LEAVE",leaveRect.x+30,leaveRect.y+28);font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();}
}
