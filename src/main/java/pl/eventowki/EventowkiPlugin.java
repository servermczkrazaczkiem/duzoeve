package pl.eventowki;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.UseCooldownComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.io.*;
import java.util.*;

public final class EventowkiPlugin extends JavaPlugin implements Listener, TabCompleter {
    enum Kind {
        CZASZKA(Material.SKELETON_SKULL,"Złota czaszka",0),
        MILOSC(Material.GOLD_NUGGET,"Miłość na zabój",30),
        PIERNIK(Material.COOKIE,"Piernik",10),
        USZY(Material.RABBIT_FOOT,"Uszy królika",30),
        LASKA(Material.SUGAR_CANE,"Laska cukrowa",0),
        BANAN(Material.YELLOW_DYE,"Skórka banana",30),
        ROZGA(Material.STICK,"Rózga 1",30),
        KLATA(Material.CHAINMAIL_CHESTPLATE,"Gwiezdna klata",0),
        MACZUGA(Material.WOODEN_SWORD,"Maczuga",30),
        JABLKO(Material.GOLDEN_APPLE,"Smocze jabłko",0),
        DYNIA(Material.PUMPKIN,"Granat dyniowy",30),
        HAK(Material.FISHING_ROD,"Hak",30),
        TORBA(Material.RABBIT_FOOT,"Torba kangura",0),
        LASSO(Material.LEAD,"Lasso",30),
        ROZDZKA(Material.END_ROD,"Różdżka wiedźmy",30),
        GRANAT(Material.SPLASH_POTION,"Granat odepchnięcia",30);
        final Material material; final String display; final int seconds;
        Kind(Material m,String n,int s){material=m;display=n;seconds=s;}
        String id(){return name().toLowerCase(Locale.ROOT);}
    }
    private NamespacedKey itemKey,bagKey,projectileKey,tempKey,caneKey,absKey;
    private YamlConfiguration data;
    private File dataFile;
    private int tick;
    private final Map<String,Long> cooldowns=new HashMap<>();
    private final Map<UUID,Long> noGlide=new HashMap<>(),noJump=new HashMap<>();
    private final Map<UUID,Location> frozen=new HashMap<>();
    private final Map<UUID,Long> freezeEnd=new HashMap<>();
    private final Map<UUID,Link> links=new HashMap<>();
    private final Map<UUID,Integer> clickTicks=new HashMap<>();
    private final Map<String,Inventory> bags=new HashMap<>();
    private final Set<UUID> spawnedProjectiles=new HashSet<>();
    private static final class Link {
        final UUID target;final long end;long next;
        Link(UUID t,long e){target=t;end=e;}
    }
    private static final class BagView implements InventoryHolder {
        final String id; Inventory inventory;
        BagView(String id){this.id=id;}
        @Override public Inventory getInventory(){return inventory;}
    }
    @Override public void onEnable(){
        saveDefaultConfig();
        itemKey=key("event_item");bagKey=key("bag_id");projectileKey=key("projectile");
        tempKey=key("temporary_pumpkin");caneKey=key("candy_health");absKey=key("love_absorption");
        dataFile=new File(getDataFolder(),"data.yml");data=YamlConfiguration.loadConfiguration(dataFile);
        var section=data.getConfigurationSection("cooldowns");
        if(section!=null)for(String k:section.getKeys(false))cooldowns.put(k,section.getLong(k));
        Bukkit.getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("eventitems")).setTabCompleter(this);
        Bukkit.getScheduler().runTaskTimer(this,this::update,1,1);
        for(Player p:Bukkit.getOnlinePlayers()){restorePumpkin(p);syncCooldowns(p);}
        getLogger().info("Eventowki 2.0.0: zaladowano 16 przedmiotow.");
    }
    private NamespacedKey key(String id){return new NamespacedKey(this,id);}
    private long now(){return System.currentTimeMillis();}
    private Kind kind(ItemStack i){
        if(i==null||!i.hasItemMeta())return null;
        String id=i.getItemMeta().getPersistentDataContainer().get(itemKey,PersistentDataType.STRING);
        if(id==null)return null;try{return Kind.valueOf(id.toUpperCase(Locale.ROOT));}catch(Exception e){return null;}
    }
    private boolean temporary(ItemStack i){return i!=null&&i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(tempKey,PersistentDataType.BYTE);}
    private String stringTag(ItemStack i,NamespacedKey k){return i!=null&&i.hasItemMeta()?i.getItemMeta().getPersistentDataContainer().get(k,PersistentDataType.STRING):null;}
    private ItemStack create(Kind k){
        ItemStack i=new ItemStack(k.material);ItemMeta m=i.getItemMeta();
        m.displayName(Component.text(k.display).color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        m.getPersistentDataContainer().set(itemKey,PersistentDataType.STRING,k.id());
        UseCooldownComponent cd=m.getUseCooldown();cd.setCooldownSeconds(Math.max(1,k.seconds));cd.setCooldownGroup(key("cd_"+k.id()));m.setUseCooldown(cd);
        List<String> lore=new ArrayList<>();
        switch(k){
            case CZASZKA -> lore.add("Śmierć: zachowaj ekwipunek. Zużywa 1 sztukę.");
            case MILOSC -> lore.add("PPM: pełne zdrowie i 9 złotych serc do zbicia.");
            case PIERNIK -> lore.add("PPM: regeneracja III na 4 s. Jednorazowy.");
            case USZY -> lore.add("Trafienie: obrót 180° i brak skoku 5 s. Jednorazowy.");
            case LASKA -> lore.add("Główna lub druga ręka: +4 czerwone serca.");
            case BANAN -> lore.add("Trafienie: wymieszaj pasek celu. Jednorazowy.");
            case ROZGA -> lore.add("Trafienie nieszybującego gracza: blokada elytry 5 s.");
            case KLATA -> {m.setUnbreakable(true);m.addEnchant(Enchantment.PROTECTION,5,true);lore.add("Założona: siła I i szybkość I.");}
            case MACZUGA -> {m.setUnbreakable(true);lore.add("PPM: śnieżka, trafiony stoi w miejscu 3 s.");}
            case JABLKO -> lore.add("Nieskończone. 10% szans na 4 zamiast 2 złotych serc.");
            case DYNIA -> lore.add("PPM: rzut jak potka. Dynie na głowach do 5 kratek przez 5 s.");
            case HAK -> {m.setUnbreakable(true);lore.add("PPM: przyciągnięcie w kierunku rzutu.");}
            case TORBA -> {m.getPersistentDataContainer().set(bagKey,PersistentDataType.STRING,UUID.randomUUID().toString());lore.add("PPM: 27 miejsc. Automatycznie zbiera cenne łupy zabójstw.");}
            case LASSO -> {lore.add("PPM na cel: więź 5 s. Dystans 10: teleport.");lore.add("Drugi PPM lub koniec 5 s: ostatni teleport do Ciebie.");}
            case ROZDZKA -> lore.add("PPM: lewitacja obszarowa 5 s. LPM: tylko cel.");
            case GRANAT -> lore.add("PPM: nieskończony granat. Silny odrzut od wybuchu.");
        }
        if(k.seconds>0)lore.add("Odnowienie: "+k.seconds+" s.");
        m.lore(lore.stream().map(s->Component.text(s).color(net.kyori.adventure.text.format.NamedTextColor.GRAY)).toList());
        if(k==Kind.GRANAT&&m instanceof PotionMeta pm)pm.setBasePotionType(PotionType.SLOW_FALLING);
        i.setItemMeta(m);return i;
    }
    private String cdId(Player p,Kind k){return p.getUniqueId()+"_"+k.id();}
    private boolean ready(Player p,Kind k){
        long left=cooldowns.getOrDefault(cdId(p,k),0L)-now();
        if(left<=0)return true;
        p.sendActionBar(Component.text("Odnowienie: "+(long)Math.ceil(left/1000.0)+" s"));return false;
    }
    private void startCooldown(Player p,Kind k,ItemStack item,boolean visual){
        cooldowns.put(cdId(p,k),now()+k.seconds*1000L);
        if(visual)p.setCooldown(item,k.seconds*20);
        saveData();
    }
    private void overlay(Player p,Kind k){
        long left=cooldowns.getOrDefault(cdId(p,k),0L)-now();
        if(left>0)p.setCooldown(create(k),(int)Math.ceil(left/50.0));
    }
    private void syncCooldowns(Player p){for(Kind k:Kind.values())if(k.seconds>0)overlay(p,k);}
    private void consume(Player p){
        ItemStack i=p.getInventory().getItemInMainHand();
        if(i.getAmount()<=1)p.getInventory().setItemInMainHand(null);else i.setAmount(i.getAmount()-1);
    }
    private void effect(Player p,PotionEffectType type,int ticks,int level){p.addPotionEffect(new PotionEffect(type,ticks,level,false,true,true));}
    private boolean pair(Player a,Player b){return a!=null&&b!=null&&a.isOnline()&&b.isOnline()&&!a.isDead()&&!b.isDead()&&!a.equals(b)&&a.getWorld().equals(b.getWorld());}
    private Player target(Player p){
        RayTraceResult r=p.getWorld().rayTrace(p.getEyeLocation(),p.getEyeLocation().getDirection(),30,FluidCollisionMode.NEVER,true,.3,e->e instanceof Player t&&!t.equals(p)&&!t.isDead());
        return r!=null&&r.getHitEntity() instanceof Player t?t:null;
    }
    @EventHandler public void interact(PlayerInteractEvent e){
        Kind k=kind(e.getItem());if(k==null)return;
        // Jedzenie jabłka pozostaje zwykłą animacją jedzenia.
        if(k==Kind.JABLKO)return;
        if(k==Kind.KLATA)return;
        if(e.getHand()!=EquipmentSlot.HAND){e.setCancelled(true);return;}
        Action a=e.getAction();boolean right=a==Action.RIGHT_CLICK_AIR||a==Action.RIGHT_CLICK_BLOCK;
        boolean left=a==Action.LEFT_CLICK_AIR||a==Action.LEFT_CLICK_BLOCK;
        if(!left&&!right)return;e.setCancelled(true);
        Player p=e.getPlayer();
        if(right)right(p,k,null);else if(k==Kind.ROZDZKA)wandSingle(p,target(p));
    }
    @EventHandler(ignoreCancelled=true) public void entityInteract(PlayerInteractEntityEvent e){
        if(e.getHand()!=EquipmentSlot.HAND)return;Player p=e.getPlayer();Kind k=kind(p.getInventory().getItemInMainHand());
        if(k==null||k==Kind.JABLKO||k==Kind.KLATA)return;e.setCancelled(true);
        right(p,k,e.getRightClicked() instanceof Player t?t:null);
    }
    private void right(Player p,Kind k,Player direct){
        if(Objects.equals(clickTicks.get(p.getUniqueId()),tick))return;clickTicks.put(p.getUniqueId(),tick);
        ItemStack hand=p.getInventory().getItemInMainHand();
        if(k==Kind.TORBA){openBag(p,hand);return;}
        if(k==Kind.LASSO){lasso(p,direct,hand);return;}
        if(!ready(p,k))return;
        switch(k){
            case MILOSC -> {
                AttributeInstance health=p.getAttribute(Attribute.MAX_HEALTH);if(health!=null)p.setHealth(health.getValue());
                AttributeInstance absorption=p.getAttribute(Attribute.MAX_ABSORPTION);
                if(absorption==null)return;
                if(absorption.getModifier(absKey)==null)absorption.addModifier(new AttributeModifier(absKey,18,AttributeModifier.Operation.ADD_NUMBER));
                p.setAbsorptionAmount(18);startCooldown(p,k,hand,true);
            }
            case PIERNIK -> {effect(p,PotionEffectType.REGENERATION,80,2);startCooldown(p,k,hand,true);consume(p);}
            case ROZDZKA -> {
                effect(p,PotionEffectType.LEVITATION,100,0);
                for(Player t:p.getWorld().getPlayers())if(!p.equals(t)&&!t.isDead()&&t.getLocation().distanceSquared(p.getLocation())<=100)effect(t,PotionEffectType.LEVITATION,100,0);
                startCooldown(p,k,hand,true);
            }
            case DYNIA,GRANAT -> {
                ThrownPotion projectile=p.launchProjectile(ThrownPotion.class);
                ItemStack visual=new ItemStack(Material.SPLASH_POTION);
                PotionMeta pm=(PotionMeta)visual.getItemMeta();pm.setColor(k==Kind.DYNIA?Color.ORANGE:Color.PURPLE);visual.setItemMeta(pm);
                projectile.setItem(visual);tag(projectile,k.id());
                startCooldown(p,k,hand,true);if(k==Kind.DYNIA)consume(p);
            }
            case MACZUGA -> {Snowball ball=p.launchProjectile(Snowball.class);tag(ball,"maczuga");startCooldown(p,k,hand,true);}
            case HAK -> {
                Snowball visual=p.launchProjectile(Snowball.class);tag(visual,"hak");
                Vector direction=p.getEyeLocation().getDirection().normalize();
                p.setVelocity(direction.multiply(getConfig().getDouble("hook.strength",2.0)));
                startCooldown(p,k,hand,true);
            }
            default -> {}
        }
    }
    private void tag(Projectile p,String type){p.getPersistentDataContainer().set(projectileKey,PersistentDataType.STRING,type);spawnedProjectiles.add(p.getUniqueId());}
    private void wandSingle(Player p,Player t){
        if(!ready(p,Kind.ROZDZKA)||!pair(p,t))return;
        effect(t,PotionEffectType.LEVITATION,100,0);startCooldown(p,Kind.ROZDZKA,p.getInventory().getItemInMainHand(),true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void hit(EntityDamageByEntityEvent e){
        if(e.getDamager() instanceof Projectile projectile&&projectile.getPersistentDataContainer().has(projectileKey,PersistentDataType.STRING)){e.setCancelled(true);return;}
        if(!(e.getDamager() instanceof Player p)||!(e.getEntity() instanceof Player t))return;
        ItemStack hand=p.getInventory().getItemInMainHand();Kind k=kind(hand);if(k==null)return;
        if(k==Kind.ROZDZKA){e.setCancelled(true);wandSingle(p,t);return;}
        if(k!=Kind.USZY&&k!=Kind.BANAN&&k!=Kind.ROZGA)return;
        if(e.getFinalDamage()<=0||!ready(p,k))return;
        if(k==Kind.ROZGA&&t.isGliding())return;
        switch(k){
            case USZY -> {Location l=t.getLocation();l.setYaw(l.getYaw()+180);t.teleport(l,PlayerTeleportEvent.TeleportCause.PLUGIN);noJump.put(t.getUniqueId(),now()+5000);}
            case BANAN -> {
                List<ItemStack> slots=new ArrayList<>();for(int i=0;i<9;i++)slots.add(t.getInventory().getItem(i));Collections.shuffle(slots);
                for(int i=0;i<9;i++)t.getInventory().setItem(i,slots.get(i));
            }
            case ROZGA -> {lockGlide(t,now()+5000);t.getWorld().playSound(t.getLocation(),Sound.ENTITY_SPLASH_POTION_BREAK,1,1);}
            default -> {}
        }
        startCooldown(p,k,hand,true);if(k==Kind.USZY||k==Kind.BANAN)consume(p);
    }
    private void lockGlide(Player p,long end){
        noGlide.merge(p.getUniqueId(),end,Math::max);p.setGliding(false);
        p.setCooldown(Material.ELYTRA,(int)Math.ceil((noGlide.get(p.getUniqueId())-now())/50.0));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void glide(EntityToggleGlideEvent e){if(e.getEntity() instanceof Player p&&e.isGliding()&&noGlide.getOrDefault(p.getUniqueId(),0L)>now())e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void jump(PlayerJumpEvent e){if(noJump.getOrDefault(e.getPlayer().getUniqueId(),0L)>now())e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void move(PlayerMoveEvent e){
        Player p=e.getPlayer();Location anchor=frozen.get(p.getUniqueId());
        if(anchor==null||freezeEnd.getOrDefault(p.getUniqueId(),0L)<=now()||e.getTo()==null)return;
        if(!anchor.getWorld().equals(e.getTo().getWorld()))return;
        Location to=anchor.clone();to.setYaw(e.getTo().getYaw());to.setPitch(e.getTo().getPitch());e.setTo(to);
    }
    @EventHandler(ignoreCancelled=true) public void teleportEvent(PlayerTeleportEvent e){frozen.remove(e.getPlayer().getUniqueId());freezeEnd.remove(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void projectileHit(ProjectileHitEvent e){
        String type=e.getEntity().getPersistentDataContainer().get(projectileKey,PersistentDataType.STRING);
        if(type==null)return;
        spawnedProjectiles.remove(e.getEntity().getUniqueId());
        if(type.equals("maczuga")&&e.getHitEntity() instanceof Player t){
            if(!(e.getEntity().getShooter() instanceof Player p)||!p.equals(t)){
                if(!t.isInsideVehicle()){frozen.put(t.getUniqueId(),t.getLocation());freezeEnd.put(t.getUniqueId(),now()+3000);}
            }
        }
        if(type.equals("maczuga")||type.equals("hak"))e.getEntity().remove();
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void splash(PotionSplashEvent e){
        String type=e.getPotion().getPersistentDataContainer().get(projectileKey,PersistentDataType.STRING);if(type==null)return;
        for(LivingEntity living:e.getAffectedEntities())e.setIntensity(living,0);
        Location center=e.getPotion().getLocation();
        Player shooter=e.getPotion().getShooter() instanceof Player p?p:null;
        if(type.equals("dynia")){
            double radius=getConfig().getDouble("pumpkin.radius",5);
            for(Player p:center.getWorld().getPlayers())if(!p.isDead()&&p.getLocation().distanceSquared(center)<=radius*radius)putPumpkin(p);
        }else if(type.equals("granat")){
            double radius=getConfig().getDouble("grenade.radius",6);
            double strength=getConfig().getDouble("grenade.horizontal-strength",3.0);
            double up=getConfig().getDouble("grenade.vertical-strength",0.8);
            for(Player p:center.getWorld().getPlayers()){
                if(p.isDead()||p.equals(shooter))continue;
                Vector delta=p.getLocation().toVector().subtract(center.toVector());double distance=delta.length();
                if(distance>=radius)continue;double factor=1-distance/radius;
                delta.setY(0);if(delta.lengthSquared()<.001)delta=new Vector(1,0,0);
                Vector v=delta.normalize().multiply(strength*factor);v.setY(up*factor);p.setVelocity(v);
            }
        }
        spawnedProjectiles.remove(e.getPotion().getUniqueId());
    }
    private void lasso(Player p,Player direct,ItemStack hand){
        Link old=links.remove(p.getUniqueId());
        if(old!=null){if(old.end>now()){Player t=Bukkit.getPlayer(old.target);if(pair(p,t))bring(p,t);}overlay(p,Kind.LASSO);return;}
        if(!ready(p,Kind.LASSO))return;Player t=direct!=null?direct:target(p);
        if(!pair(p,t)||!p.hasLineOfSight(t)||p.getEyeLocation().distanceSquared(t.getEyeLocation())>900){p.sendMessage("§cCeluj w gracza do 30 kratek.");return;}
        links.put(p.getUniqueId(),new Link(t.getUniqueId(),now()+5000));
        startCooldown(p,Kind.LASSO,hand,false);lockGlide(p,now()+5000);lockGlide(t,now()+5000);
        p.sendMessage("§6Lasso: "+t.getName()+" — 5 sekund.");
    }
    private void bring(Player owner,Player target){
        if(!pair(owner,target))return;Location d=owner.getLocation().clone();d.setYaw(target.getLocation().getYaw());d.setPitch(target.getLocation().getPitch());
        if(target.teleport(d,PlayerTeleportEvent.TeleportCause.PLUGIN)){target.setVelocity(new Vector());target.setFallDistance(0);d.getWorld().playSound(d,Sound.ENTITY_ENDERMAN_TELEPORT,.8f,1.2f);}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void eat(PlayerItemConsumeEvent e){
        if(kind(e.getItem())!=Kind.JABLKO)return;
        e.setCancelled(true);Player p=e.getPlayer();int hearts=Math.random()<.1?4:2;
        p.setFoodLevel(Math.min(20,p.getFoodLevel()+4));p.setSaturation(Math.min(p.getFoodLevel(),p.getSaturation()+9.6f));
        effect(p,PotionEffectType.REGENERATION,100,1);
        effect(p,PotionEffectType.ABSORPTION,2400,hearts==4?1:0);
        p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(),hearts*2));
        p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_BURP,1,1);
    }
    private void putPumpkin(Player p){
        if(p.isDead())return;String path="pumpkins."+p.getUniqueId();
        if(data.contains(path)){data.set(path+".end",now()+5000);saveData();return;}
        ItemStack helmet=p.getInventory().getHelmet();
        if(helmet!=null&&!helmet.getType().isAir()&&p.getInventory().firstEmpty()==-1){p.sendMessage("§eDynia: brak miejsca na bezpieczne schowanie hełmu.");return;}
        if(helmet!=null&&!helmet.getType().isAir()){p.getInventory().addItem(helmet.clone());data.set(path+".helmet",helmet.clone());}
        data.set(path+".end",now()+5000);
        ItemStack pumpkin=new ItemStack(Material.CARVED_PUMPKIN);ItemMeta m=pumpkin.getItemMeta();
        m.displayName(Component.text("Dynia granatu"));m.addEnchant(Enchantment.VANISHING_CURSE,1,true);
        m.getPersistentDataContainer().set(tempKey,PersistentDataType.BYTE,(byte)1);pumpkin.setItemMeta(m);
        p.getInventory().setHelmet(pumpkin);saveData();
    }
    private void restorePumpkin(Player p){
        String path="pumpkins."+p.getUniqueId();if(!data.contains(path))return;
        if(temporary(p.getInventory().getHelmet()))p.getInventory().setHelmet(null);
        for(int i=0;i<p.getInventory().getSize();i++)if(temporary(p.getInventory().getItem(i)))p.getInventory().setItem(i,null);
        ItemStack original=data.getItemStack(path+".helmet");
        ItemStack current=p.getInventory().getHelmet();
        if(original!=null&&(current==null||current.getType().isAir())){
            for(int i=0;i<36;i++){
                ItemStack found=p.getInventory().getItem(i);
                if(found!=null&&found.isSimilar(original)){
                    ItemStack one=found.clone();one.setAmount(1);p.getInventory().setHelmet(one);
                    if(found.getAmount()==1)p.getInventory().setItem(i,null);else found.setAmount(found.getAmount()-1);break;
                }
            }
        }
        data.set(path,null);saveData();
    }
    @EventHandler public void drop(PlayerDropItemEvent e){if(temporary(e.getItemDrop().getItemStack()))e.setCancelled(true);}
    @EventHandler public void drag(InventoryDragEvent e){
        if(e.getView().getTopInventory().getHolder() instanceof BagView){e.setCancelled(true);return;}
        if(temporary(e.getOldCursor()))e.setCancelled(true);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(e.getView().getTopInventory().getHolder() instanceof BagView view){
            e.setCancelled(true);
            if(!(e.getWhoClicked() instanceof Player p)||e.getRawSlot()<0)return;
            if(!ownsBag(p,view.id))return;
            if(e.getClick()!=ClickType.LEFT && e.getClick()!=ClickType.RIGHT && e.getClick()!=ClickType.SHIFT_LEFT && e.getClick()!=ClickType.SHIFT_RIGHT)return;
            Inventory storage=bag(view.id);
            if(e.getRawSlot()>=27){
                if(e.getClickedInventory()!=p.getInventory())return;
                ItemStack deposit=e.getCurrentItem();
                if(deposit==null||temporary(deposit)||kind(deposit)==Kind.TORBA)return;
                Map<Integer,ItemStack> left=storage.addItem(deposit.clone());
                e.setCurrentItem(left.isEmpty()?null:left.values().iterator().next());
            }else{
                int slot=e.getRawSlot();ItemStack item=storage.getItem(slot);if(item==null)return;
                Map<Integer,ItemStack> left=p.getInventory().addItem(item.clone());
                storage.setItem(slot,left.isEmpty()?null:left.values().iterator().next());
            }
            saveBag(view.id);refreshBagViews(view.id);return;
        }
        if(temporary(e.getCurrentItem())||temporary(e.getCursor()))e.setCancelled(true);
    }
    private Inventory bag(String id){
        return bags.computeIfAbsent(id,k->{Inventory inv=Bukkit.createInventory(null,27);
            List<?> content=data.getList("bags."+k);
            if(content!=null)for(int i=0;i<Math.min(27,content.size());i++)if(content.get(i) instanceof ItemStack it)inv.setItem(i,it.clone());return inv;});
    }
    private void saveBag(String id){data.set("bags."+id,Arrays.asList(bag(id).getContents()));saveData();}
    private boolean ownsBag(Player p,String id){for(ItemStack item:p.getInventory().getContents())if(kind(item)==Kind.TORBA&&id.equals(stringTag(item,bagKey)))return true;return false;}
    private void openBag(Player p,ItemStack item){
        String id=stringTag(item,bagKey);if(id==null)return;
        BagView view=new BagView(id);view.inventory=Bukkit.createInventory(view,27,Component.text("Torba kangura"));
        view.inventory.setContents(bag(id).getContents());p.openInventory(view.inventory);
    }
    private void refreshBagViews(String id){for(Player p:Bukkit.getOnlinePlayers())if(p.getOpenInventory().getTopInventory().getHolder() instanceof BagView v&&v.id.equals(id))v.inventory.setContents(bag(id).getContents());}
    private boolean valuable(ItemStack item){
        if(item==null||temporary(item))return false;
        if(kind(item)!=null)return true;String n=item.getType().name();
        return n.endsWith("_HELMET")||n.endsWith("_CHESTPLATE")||n.endsWith("_LEGGINGS")||n.endsWith("_BOOTS")||n.endsWith("_SWORD")||n.endsWith("_AXE")||item.getType()==Material.ELYTRA;
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void death(PlayerDeathEvent e){
        Player p=e.getEntity();UUID uuid=p.getUniqueId();
        if(p.getOpenInventory().getTopInventory().getHolder() instanceof BagView)p.closeInventory();
        boolean protectedInventory=false;
        if(!e.getKeepInventory()){
            for(int slot=0;slot<p.getInventory().getSize();slot++){
                ItemStack i=p.getInventory().getItem(slot);
                if(kind(i)==Kind.CZASZKA){
                    if(i.getAmount()==1)p.getInventory().setItem(slot,null);else i.setAmount(i.getAmount()-1);
                    e.setKeepInventory(true);e.getDrops().clear();protectedInventory=true;break;
                }
            }
        }
        e.getDrops().removeIf(this::temporary);
        if(protectedInventory&&temporary(p.getInventory().getHelmet()))p.getInventory().setHelmet(null);
        data.set("pumpkins."+uuid,null);
        removeModifier(p,Attribute.MAX_ABSORPTION,absKey);p.setAbsorptionAmount(0);
        frozen.remove(uuid);freezeEnd.remove(uuid);noJump.remove(uuid);noGlide.remove(uuid);
        Player killer=p.getKiller();
        if(!e.getKeepInventory()&&killer!=null&&!killer.equals(p)){
            String id=null;for(ItemStack item:killer.getInventory().getContents())if(kind(item)==Kind.TORBA){id=stringTag(item,bagKey);if(id!=null)break;}
            if(id!=null){
                Inventory inv=bag(id);ListIterator<ItemStack> iterator=e.getDrops().listIterator();
                while(iterator.hasNext()){
                    ItemStack item=iterator.next();if(!valuable(item))continue;
                    Map<Integer,ItemStack> left=inv.addItem(item.clone());iterator.remove();
                    for(ItemStack rest:left.values())iterator.add(rest);
                }
                saveBag(id);refreshBagViews(id);
            }
        }
        saveData();
    }
    @EventHandler(ignoreCancelled=true) public void damageItem(PlayerItemDamageEvent e){Kind k=kind(e.getItem());if(k==Kind.KLATA||k==Kind.MACZUGA||k==Kind.HAK)e.setCancelled(true);}
    private void removeModifier(Player p,Attribute attribute,NamespacedKey key){AttributeInstance a=p.getAttribute(attribute);if(a!=null&&a.getModifier(key)!=null)a.removeModifier(a.getModifier(key));}
    private void passive(Player p){
        AttributeInstance a=p.getAttribute(Attribute.MAX_HEALTH);if(a!=null){
            boolean held=kind(p.getInventory().getItemInMainHand())==Kind.LASKA||kind(p.getInventory().getItemInOffHand())==Kind.LASKA;
            if(held&&a.getModifier(caneKey)==null)a.addModifier(new AttributeModifier(caneKey,8,AttributeModifier.Operation.ADD_NUMBER));
            if(!held&&a.getModifier(caneKey)!=null){a.removeModifier(a.getModifier(caneKey));if(!p.isDead()&&p.getHealth()>a.getValue())p.setHealth(a.getValue());}
        }
        if(!p.isDead()&&kind(p.getInventory().getChestplate())==Kind.KLATA){effect(p,PotionEffectType.STRENGTH,20,0);effect(p,PotionEffectType.SPEED,20,0);}
        if(p.getAbsorptionAmount()<=0)removeModifier(p,Attribute.MAX_ABSORPTION,absKey);
    }
    private void update(){
        tick++;long time=now();
        for(UUID uuid:List.copyOf(links.keySet())){
            Link l=links.get(uuid);if(l==null)continue;Player p=Bukkit.getPlayer(uuid),t=Bukkit.getPlayer(l.target);
            if(!pair(p,t)){links.remove(uuid);if(p!=null&&p.isOnline())overlay(p,Kind.LASSO);continue;}
            if(time>=l.end){links.remove(uuid);bring(p,t);overlay(p,Kind.LASSO);p.sendActionBar(Component.text("Lasso: ostatni teleport"));continue;}
            if(tick%5==0)p.sendActionBar(Component.text("Lasso: "+(long)Math.ceil((l.end-time)/1000.0)+" s | PPM: koniec"));
            if(p.getLocation().distanceSquared(t.getLocation())>=100&&time>=l.next){l.next=time+250;bring(p,t);}
        }
        noGlide.entrySet().removeIf(e->e.getValue()<=time);noJump.entrySet().removeIf(e->e.getValue()<=time);
        for(UUID uuid:List.copyOf(noGlide.keySet())){Player p=Bukkit.getPlayer(uuid);if(p!=null&&p.isGliding())p.setGliding(false);}
        for(UUID uuid:List.copyOf(freezeEnd.keySet()))if(freezeEnd.get(uuid)<=time){freezeEnd.remove(uuid);frozen.remove(uuid);}
        if(tick%5==0)for(Player p:Bukkit.getOnlinePlayers()){
            passive(p);String path="pumpkins."+p.getUniqueId();
            if(data.contains(path)&&data.getLong(path+".end")<=time)restorePumpkin(p);
        }
        if(tick%200==0){cooldowns.entrySet().removeIf(e->e.getValue()<=time);clickTicks.clear();spawnedProjectiles.removeIf(u->Bukkit.getEntity(u)==null);}
        if(tick%1200==0)saveData();
    }
    @EventHandler public void join(PlayerJoinEvent e){Player p=e.getPlayer();restorePumpkin(p);Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()){syncCooldowns(p);passive(p);}},2);}
    @EventHandler public void quit(PlayerQuitEvent e){
        Player p=e.getPlayer();restorePumpkin(p);removeModifier(p,Attribute.MAX_HEALTH,caneKey);
        Link l=links.remove(p.getUniqueId());if(l!=null)overlay(p,Kind.LASSO);
        frozen.remove(p.getUniqueId());freezeEnd.remove(p.getUniqueId());saveData();
    }
    @Override public void onDisable(){
        for(Player p:Bukkit.getOnlinePlayers()){if(p.getOpenInventory().getTopInventory().getHolder() instanceof BagView)p.closeInventory();restorePumpkin(p);removeModifier(p,Attribute.MAX_HEALTH,caneKey);}
        for(UUID u:spawnedProjectiles){Entity e=Bukkit.getEntity(u);if(e!=null)e.remove();}
        if(data!=null)saveData();
    }
    private void saveData(){
        data.set("cooldowns",null);for(var e:cooldowns.entrySet())if(e.getValue()>now())data.set("cooldowns."+e.getKey(),e.getValue());
        try{data.save(dataFile);}catch(IOException e){getLogger().severe("Nie mozna zapisac data.yml: "+e.getMessage());}
    }
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){
        if(!sender.isOp()&&!sender.hasPermission("eventitems.admin")){sender.sendMessage("Brak uprawnień.");return true;}
        if(args.length==0||args[0].equalsIgnoreCase("lista")){sender.sendMessage("/eventitems <id|all> [nick] [ilość]");for(Kind k:Kind.values())sender.sendMessage(k.id()+" — "+k.display);return true;}
        Player p=args.length>=2?Bukkit.getPlayerExact(args[1]):sender instanceof Player player?player:null;
        if(p==null){sender.sendMessage("Podaj nick gracza online.");return true;}
        int count=1;try{if(args.length>=3)count=Math.max(1,Math.min(64,Integer.parseInt(args[2])));}catch(NumberFormatException e){sender.sendMessage("Ilość musi być liczbą 1–64.");return true;}
        List<Kind> requested;
        if(args[0].equalsIgnoreCase("all"))requested=List.of(Kind.values());else{
            try{requested=List.of(Kind.valueOf(args[0].toUpperCase(Locale.ROOT)));}catch(IllegalArgumentException e){sender.sendMessage("Nieznany przedmiot. /eventitems lista");return true;}
        }
        for(Kind k:requested){
            if(k==Kind.TORBA){for(int n=0;n<count;n++)give(p,create(k));}
            else {ItemStack item=create(k);item.setAmount(Math.min(count,item.getMaxStackSize()));give(p,item);}
            if(k.seconds>0)overlay(p,k);
        }
        sender.sendMessage("Przekazano przedmioty graczowi "+p.getName()+".");return true;
    }
    private void give(Player p,ItemStack item){for(ItemStack left:p.getInventory().addItem(item).values())p.getWorld().dropItemNaturally(p.getLocation(),left);}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] args){
        if(!s.isOp()&&!s.hasPermission("eventitems.admin"))return List.of();
        if(args.length==1){List<String> ids=new ArrayList<>(List.of("all","lista"));for(Kind k:Kind.values())ids.add(k.id());return ids.stream().filter(x->x.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();}
        if(args.length==2)return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(x->x.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();return List.of();
    }
}
