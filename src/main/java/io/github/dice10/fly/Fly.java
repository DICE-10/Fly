package io.github.dice10.fly;


import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class Fly extends JavaPlugin implements Listener {

    private CommandSender sender;
    private Command cmd;
    private String commandLabel;
    private String[] args;
    private final List<UUID> allowed;
    private  int flg = 0;
    private int time;
    private int cost;
    Runnable task;
    ScheduledExecutorService scheduler ;
    ScheduledFuture future;

    public Fly() {
        this.allowed = new ArrayList<>();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        getServer().getPluginManager().registerEvents(this,this);


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public  void Logout(PlayerQuitEvent event){
        event.getPlayer().setAllowFlight(false);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        Player player = null;
        try {
            if (sender instanceof Player) {
                // プレイヤーが実行
                player = (Player) sender;
            } else {
                // コンソールが実行
                sender.sendMessage("  \n/fly [消費経験値]");
                // 処理を終わらせる
                return true;
            }
            if(args.length == 1){
                if(args[0].equals("help")){
                    sender.sendMessage(ChatColor.YELLOW +"/fly help --ヘルプ表示");
                    sender.sendMessage(ChatColor.YELLOW +"/fly [飛行時間(秒)]");
                    sender.sendMessage(ChatColor.YELLOW +"/fly [飛行時間(秒)] check exp --飛行時間による消費経験値の表示");
                    sender.sendMessage(ChatColor.YELLOW +"飛行時間が過ぎる前にもう一度コマンドを実行すると飛行時間が上書きされ、\n実行時からの秒数となり追加されるわけではありません。");
                }
                else if(args[0].equals("reload")){
                    reloadConfig();
                    sender.sendMessage(ChatColor.RED + "The Fly plugin config file is reloaded.");
                }
                else if(isNumeric(args[0]) && Integer.parseInt(args[0]) > 0){
                    int time = Integer.parseInt(args[0]);
                    int cost = time * this.getConfig().getInt("cost");
                    int playerExp = ((Player) sender).getTotalExperience();
                    if(playerExp < cost){
                        sender.sendMessage(ChatColor.YELLOW + "経験値が足りません!!");
                        return false;
                    }
                    if(playerExp - cost >= 0){
                        ((Player) sender).giveExp(-(cost));
                        ((Player) sender).setAllowFlight(true);
                        BukkitRunnable task = new BukkitRunnable() {
                            int runTime = time;
                            @Override
                            public void run() {
                                if (runTime == 0) {
                                    ((Player) sender).setAllowFlight(false);
                                    sender.sendMessage(ChatColor.RED + "■  " + ChatColor.YELLOW + "Fly mode OFF.");
                                    sender.sendMessage(ChatColor.YELLOW + "10秒間低速落下します。");
                                    ((Player) sender).addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,200,0));
                                    ((Player) sender).addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,200,19));
                                    cancel();
                                    return;
                                }
                                else if(runTime == 10){
                                    sender.sendMessage(ChatColor.YELLOW+"飛行終了まで残り");
                                    sender.sendMessage(ChatColor.AQUA+String.valueOf(runTime));
                                }
                                else if(runTime < 10){
                                    sender.sendMessage(ChatColor.AQUA+String.valueOf(runTime));
                                }
                                runTime--;
                            }
                        };
                        sender.sendMessage(ChatColor.GREEN + "■  " + ChatColor.YELLOW + "Fly mode ON.");
                        sender.sendMessage("約"+String.valueOf(time) + "秒間の飛行が可能です。");
                        task.runTaskTimer(this, 0L, 20L);
                    }
                }
            }
            else if(args.length > 1){
                if(args[1].equals("check") && args[2].equals("exp")){
                    time = Integer.parseInt(args[0]);
                    cost = time * this.getConfig().getInt("cost");
                    sender.sendMessage(ChatColor.DARK_GREEN+String.valueOf(time)+"秒間"+ChatColor.WHITE+"の飛行に必要な経験値は"+ChatColor.YELLOW+String.valueOf(cost)+"ポイント"+ChatColor.WHITE+"です。");
                    sender.sendMessage(ChatColor.YELLOW+"計算方法 ： 消費経験値 = 秒数 × "+ this.getConfig().getString("cost"));
                }
            }
            else {
                sender.sendMessage(ChatColor.YELLOW + "引数は1以上を入力してください。");
            }
        }catch (Exception e) {
            return true;
        }
        return false;
    }

    public static boolean isNumeric(String strNum) {
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
        }
}
