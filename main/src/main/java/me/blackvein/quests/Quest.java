/*******************************************************************************************************
 * Continued by PikaMug (formerly HappyPikachu) with permission from _Blackvein_. All rights reserved.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 * NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************************************/

package me.blackvein.quests;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.codisimus.plugins.phatloots.PhatLootsAPI;
import com.codisimus.plugins.phatloots.loot.CommandLoot;
import com.codisimus.plugins.phatloots.loot.LootBundle;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.util.player.UserManager;
import com.herocraftonline.heroes.characters.Hero;

import me.blackvein.quests.actions.Action;
import me.blackvein.quests.events.quester.QuesterPostChangeStageEvent;
import me.blackvein.quests.events.quester.QuesterPostCompleteQuestEvent;
import me.blackvein.quests.events.quester.QuesterPostFailQuestEvent;
import me.blackvein.quests.events.quester.QuesterPreChangeStageEvent;
import me.blackvein.quests.events.quester.QuesterPreCompleteQuestEvent;
import me.blackvein.quests.events.quester.QuesterPreFailQuestEvent;
import me.blackvein.quests.util.ConfigUtil;
import me.blackvein.quests.util.InventoryUtil;
import me.blackvein.quests.util.ItemUtil;
import me.blackvein.quests.util.Lang;
import me.clip.placeholderapi.PlaceholderAPI;
import net.citizensnpcs.api.npc.NPC;

public class Quest {

    protected Quests plugin;
    protected String id;
    private String name;
    protected String description;
    protected String finished;
    protected ItemStack guiDisplay = null;
    private LinkedList<Stage> orderedStages = new LinkedList<Stage>();
    protected NPC npcStart;
    protected Location blockStart;
    protected String regionStart = null;
    protected Action initialAction;
    private Requirements reqs = new Requirements();
    private Planner pln = new Planner();
    private Rewards rews = new Rewards();
    private Options opts = new Options();
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getFinished() {
        return finished;
    }
    
    public void setFinished(String finished) {
        this.finished = finished;
    }
    
    public String getRegionStart() {
        return regionStart;
    }
    
    public void setRegionStart(String regionStart) {
        this.regionStart = regionStart;
    }
    
    public ItemStack getGUIDisplay() {
        return guiDisplay;
    }
    
    public void setGUIDisplay(ItemStack guiDisplay) {
        this.guiDisplay = guiDisplay;
    }
    
    public Stage getStage(int index) {
        try {
            return orderedStages.get(index);
        } catch (Exception e) {
            return null;
        }
    }
    
    public LinkedList<Stage> getStages() {
        return orderedStages;
    }
    
    public NPC getNpcStart() {
        return npcStart;
    }
    
    public void setNpcStart(NPC npcStart) {
        this.npcStart = npcStart;
    }
    
    public Location getBlockStart() {
        return blockStart;
    }
    
    public void setBlockStart(Location blockStart) {
        this.blockStart = blockStart;
    }
    
    public Action getInitialAction() {
        return initialAction;
    }
    
    public void setInitialAction(Action initialAction) {
        this.initialAction = initialAction;
    }
    
    public Requirements getRequirements() {
        return reqs;
    }
    
    public Planner getPlanner() {
        return pln;
    }
    
    public Rewards getRewards() {
        return rews;
    }
    
    public Options getOptions() {
        return opts;
    }

    /**
     * Force player to proceed to the next ordered stage
     * 
     * @param quester Player to force
     * @param allowSharedProgress Whether to distribute progress to fellow questers
     */
    public void nextStage(Quester quester, boolean allowSharedProgress) {
        Stage currentStage = quester.getCurrentStage(this);
        if (currentStage == null) {
            plugin.getLogger().severe("Current stage was null for quester " + quester.getPlayer().getUniqueId());
            return;
        }
        String stageCompleteMessage = currentStage.completeMessage;
        if (stageCompleteMessage != null) {
            if (quester.getOfflinePlayer().isOnline()) {
                quester.getPlayer().sendMessage(ConfigUtil.parseStringWithPossibleLineBreaks(stageCompleteMessage, 
                        this, quester.getPlayer()));
            }
        }
        if (quester.getPlayer().hasPermission("quests.compass")) {
            quester.resetCompass();
            quester.findCompassTarget();
        }
        if (currentStage.delay < 0) {
            if (currentStage.finishAction != null) {
                currentStage.finishAction.fire(quester, this);
            }
            if (quester.currentQuests.get(this) == (orderedStages.size() - 1)) {
                if (currentStage.script != null) {
                    plugin.getDenizenTrigger().runDenizenScript(currentStage.script, quester);
                }
                completeQuest(quester);
            } else {
                setStage(quester, quester.currentQuests.get(this) + 1);
            }
            if (quester.getQuestData(this) != null) {
                quester.getQuestData(this).setDelayStartTime(0);
                quester.getQuestData(this).setDelayTimeLeft(-1);
            }
            
            // Multiplayer
            if (opts.getShareProgressLevel() == 3) {
                List<Quester> mq = quester.getMultiplayerQuesters(this);
                for (Quester qq : mq) {
                    if (currentStage.equals(qq.getCurrentStage(this))) {
                        nextStage(qq, allowSharedProgress);
                    }
                }
            }
        } else {
            quester.startStageTimer(this);
        }
        quester.updateJournal();
    }

    /**
     * Force player to proceed to the specified stage
     * 
     * @param quester Player to force
     * @param stage Stage number to specify
     * @throws IndexOutOfBoundsException if stage does not exist
     */
    public void setStage(Quester quester, int stage) throws IndexOutOfBoundsException {
        OfflinePlayer player = quester.getOfflinePlayer();
        if (orderedStages.size() - 1 < stage) {
            String msg = "Tried to set invalid stage number of " + stage + " for quest " + getName() + " on " 
                    + player.getName();
            throw new IndexOutOfBoundsException(msg);
        }
        Stage currentStage = quester.getCurrentStage(this);
        Stage nextStage = getStage(stage);
        if (player.isOnline()) {
            QuesterPreChangeStageEvent preEvent = new QuesterPreChangeStageEvent(quester, this, currentStage, nextStage);
            plugin.getServer().getPluginManager().callEvent(preEvent);
            if (preEvent.isCancelled()) {
                return;
            }
        }
        quester.hardQuit(this);
        quester.hardStagePut(this, stage);
        quester.addEmptiesFor(this, stage);
        if (currentStage.script != null) {
            plugin.getDenizenTrigger().runDenizenScript(currentStage.script, quester);
        }
        if (nextStage.startAction != null) {
            nextStage.startAction.fire(quester, this);
        }
        updateCompass(quester, nextStage);
        if (player.isOnline()) {
            Player p = quester.getPlayer();
            String msg = Lang.get(p, "questObjectivesTitle");
            msg = msg.replace("<quest>", name);
            p.sendMessage(ChatColor.GOLD + msg);
            plugin.showObjectives(this, quester, false);
            String stageStartMessage = quester.getCurrentStage(this).startMessage;
            if (stageStartMessage != null) {
                p.sendMessage(ConfigUtil.parseStringWithPossibleLineBreaks(stageStartMessage, this, p));
            }
        }
        quester.updateJournal();
        if (player.isOnline()) {
            QuesterPostChangeStageEvent postEvent = new QuesterPostChangeStageEvent(quester, this, currentStage, nextStage);
            plugin.getServer().getPluginManager().callEvent(postEvent);
        }
    }

    /**
     * Set location-objective target for compass.<p>
     * 
     * Method may be called as often as needed.
     * 
     * @param quester The online quester to have their compass updated
     * @param stage The stage to process for targets
     * @return true if successful
     */
    public boolean updateCompass(Quester quester, Stage stage) {
        if (quester == null) {
            return false;
        }
        if (stage == null) {
            return false;
        }
        if (!quester.getOfflinePlayer().isOnline()) {
            return false;
        }
        if (!quester.getPlayer().hasPermission("quests.compass")) {
            return false;
        }
        Location targetLocation = null;
        if (stage.citizensToInteract != null && stage.citizensToInteract.size() > 0) {
            targetLocation = plugin.getDependencies().getNPCLocation(stage.citizensToInteract.getFirst());
        } else if (stage.citizensToKill != null && stage.citizensToKill.size() > 0) {
            targetLocation = plugin.getDependencies().getNPCLocation(stage.citizensToKill.getFirst());
        } else if (stage.locationsToReach != null && stage.locationsToReach.size() > 0) {
            targetLocation = stage.locationsToReach.getFirst();
        } else if (stage.itemDeliveryTargets != null && stage.itemDeliveryTargets.size() > 0) {
            NPC npc = plugin.getDependencies().getCitizens().getNPCRegistry().getById(stage.itemDeliveryTargets
                    .getFirst());
            targetLocation = npc.getStoredLocation();
        }
        if (targetLocation != null && targetLocation.getWorld() != null) {
            if (targetLocation.getWorld().getName().equals(quester.getPlayer().getWorld().getName())) {
                quester.getPlayer().setCompassTarget(targetLocation);
            }
        }
        return targetLocation != null;
    }
    
    /**
     * Check that a quester has met all Requirements to accept this quest<p>
     * 
     * Item, permission and custom Requirements are only checked for online players
     * 
     * @param quester The quester to check
     * @return true if all Requirements have been met
     */
    public boolean testRequirements(Quester quester) {
        return testRequirements(quester.getOfflinePlayer());
    }
    
    /**
     * Check that a player has met all Requirements to accept this quest<p>
     * 
     * Item, permission and custom Requirements are only checked for online players
     * 
     * @param player The player to check
     * @return true if all Requirements have been met
     */
    protected boolean testRequirements(OfflinePlayer player) {
        Quester quester = plugin.getQuester(player.getUniqueId());
        if (reqs.getMoney() != 0 && plugin.getDependencies().getVaultEconomy() != null) {
            if (plugin.getDependencies().getVaultEconomy().getBalance(player) < reqs.getMoney()) {
                return false;
            }
        }
        if (quester.questPoints < reqs.getQuestPoints()) {
            return false;
        }
        if (quester.completedQuests.containsAll(reqs.getNeededQuests()) == false) {
            return false;
        }
        for (String q : reqs.getBlockQuests()) {
            Quest questObject = new Quest();
            questObject.name = q;
            if (quester.completedQuests.contains(q) || quester.currentQuests.containsKey(questObject)) {
                return false;
            }
        }
        for (String s : reqs.getMcmmoSkills()) {
            final SkillType st = Quests.getMcMMOSkill(s);
            final int lvl = reqs.getMcmmoAmounts().get(reqs.getMcmmoSkills().indexOf(s));
            if (UserManager.getOfflinePlayer(player).getProfile().getSkillLevel(st) < lvl) {
                return false;
            }
        }
        if (reqs.getHeroesPrimaryClass() != null) {
            if (plugin.getDependencies()
                    .testPrimaryHeroesClass(reqs.getHeroesPrimaryClass(), player.getUniqueId()) == false) {
                return false;
            }
        }
        if (reqs.getHeroesSecondaryClass() != null) {
            if (plugin.getDependencies()
                    .testSecondaryHeroesClass(reqs.getHeroesSecondaryClass(), player.getUniqueId()) == false) {
                return false;
            }
        }
        if (player.isOnline()) {
            Player p = (Player)player;
            PlayerInventory inventory = p.getInventory();
            int num = 0;
            for (ItemStack is : reqs.getItems()) {
                for (ItemStack stack : inventory.getContents()) {
                    if (stack != null) {
                        if (ItemUtil.compareItems(is, stack, true) == 0) {
                            num += stack.getAmount();
                        }
                    }
                }
                if (num < is.getAmount()) {
                    return false;
                }
                num = 0;
            }
            for (String s : reqs.getPermissions()) {
                if (p.hasPermission(s) == false) {
                    return false;
                }
            }
            for (String s : reqs.getCustomRequirements().keySet()) {
                CustomRequirement found = null;
                for (CustomRequirement cr : plugin.getCustomRequirements()) {
                    if (cr.getName().equalsIgnoreCase(s)) {
                        found = cr;
                        break;
                    }
                }
                if (found != null) {
                    if (found.testRequirement(p, reqs.getCustomRequirements().get(s)) == false) {
                        return false;
                    }
                } else {
                    plugin.getLogger().warning("Quester \"" + p.getName() + "\" attempted to take Quest \"" + name 
                            + "\", but the Custom Requirement \"" + s + "\" could not be found. Does it still exist?");
                }
            }
        }
        return true;
    }
    
    /**
     * Proceed to finish this quest, issuing applicable rewards
     * 
     * @param q The quester finishing this quest
     */
    @SuppressWarnings("deprecation")
    public void completeQuest(Quester q) {
        OfflinePlayer player = q.getOfflinePlayer();
        if (player.isOnline()) {
            QuesterPreCompleteQuestEvent preEvent = new QuesterPreCompleteQuestEvent(q, this);
            plugin.getServer().getPluginManager().callEvent(preEvent);
            if (preEvent.isCancelled()) {
                return;
            }
        }
        q.hardQuit(this);
        if (!q.completedQuests.contains(name)) {
            q.completedQuests.add(name);
        }
        for (Map.Entry<Integer, Quest> entry : q.timers.entrySet()) {
            if (entry.getValue().getName().equals(getName())) {
                plugin.getServer().getScheduler().cancelTask(entry.getKey());
                q.timers.remove(entry.getKey());
            }
        }
        if (player.isOnline()) {
            Player p = (Player)player;
            final String[] ps = ConfigUtil.parseStringWithPossibleLineBreaks(ChatColor.AQUA + finished, this, p);
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

                @Override
                public void run() {
                    p.sendMessage(ps);
                }
            }, 40);
        }
        if (pln.getCooldown() > -1) {
            q.completedTimes.put(this.name, System.currentTimeMillis());
            if (q.amountsCompleted.containsKey(this.name)) {
                q.amountsCompleted.put(this.name, q.amountsCompleted.get(this.name) + 1);
            } else {
                q.amountsCompleted.put(this.name, 1);
            }
        }
        
        // Issue rewards
        boolean issuedReward = false;
        if (rews.getMoney() > 0 && plugin.getDependencies().getVaultEconomy() != null) {
            plugin.getDependencies().getVaultEconomy().depositPlayer(player, rews.getMoney());
            issuedReward = true;
        }
        if (player.isOnline()) {
            Player p = (Player)player;
            for (ItemStack i : rews.getItems()) {
                try {
                    InventoryUtil.addItem(p, i);
                } catch (Exception e) {
                    plugin.getLogger().severe("Unable to add null reward item to inventory of " 
                            + player.getName() + " upon completion of quest " + name);
                    p.sendMessage(ChatColor.RED + "Quests encountered a problem with an item. "
                            + "Please contact an administrator.");
                }
                issuedReward = true;
            }
        }
        for (String s : rews.getCommands()) {
            String temp = s.replace("<player>", player.getName());
            if (plugin.getDependencies().getPlaceholderApi() != null && player.isOnline()) {
                temp = PlaceholderAPI.setPlaceholders((Player)player, temp);
            }
            final String command = temp;
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            } else {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    
                    @Override
                    public void run() {
                        Bukkit.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                    }
                });
            }
            issuedReward = true;
        }
        for (int i = 0; i < rews.getPermissions().size(); i++) {
            if (plugin.getDependencies().getVaultPermission() != null) {
                String perm = rews.getPermissions().get(i);
                String world = rews.getPermissionWorlds().get(i);
                if (world == null || world.equals("null")) {
                    plugin.getDependencies().getVaultPermission().playerAdd(null, player, perm);
                } else {
                    plugin.getDependencies().getVaultPermission().playerAdd(world, player, perm);
                }
            }
            issuedReward = true;
        }
        for (String s : rews.getMcmmoSkills()) {
            UserManager.getOfflinePlayer(player).getProfile().addLevels(Quests.getMcMMOSkill(s), 
                    rews.getMcmmoAmounts().get(rews.getMcmmoSkills().indexOf(s)));
            issuedReward = true;
        }
        if (player.isOnline()) {
            for (String s : rews.getHeroesClasses()) {
                Hero hero = plugin.getDependencies().getHero(player.getUniqueId());
                hero.addExp(rews.getHeroesAmounts().get(rews.getHeroesClasses().indexOf(s)), 
                        plugin.getDependencies().getHeroes().getClassManager().getClass(s), 
                        ((Player)player).getLocation());
                issuedReward = true;
            }
        }
        LinkedList<ItemStack> phatLootItems = new LinkedList<ItemStack>();
        int phatLootExp = 0;
        LinkedList<String> phatLootMessages = new LinkedList<String>();
        for (String s : rews.getPhatLoots()) {
            LootBundle lb = PhatLootsAPI.getPhatLoot(s).rollForLoot();
            if (lb.getExp() > 0) {
                phatLootExp += lb.getExp();
                if (player.isOnline()) {
                    ((Player)player).giveExp(lb.getExp());
                }
            }
            if (lb.getMoney() > 0) {
                if (plugin.getDependencies().getVaultEconomy() != null) {
                    plugin.getDependencies().getVaultEconomy()
                            .depositPlayer(player, lb.getMoney());
                }
            }
            if (lb.getItemList().isEmpty() == false) {
                phatLootItems.addAll(lb.getItemList());
                if (player.isOnline()) {
                    Player p = (Player)player;
                    for (ItemStack is : lb.getItemList()) {
                        try {
                            InventoryUtil.addItem(p, is);
                        } catch (Exception e) {
                            plugin.getLogger().severe("Unable to add PhatLoots item to inventory of " + p.getName() 
                                    + " upon completion of quest " + name);
                            p.sendMessage(ChatColor.RED + "Quests encountered a problem with an item. "
                                    + "Please contact an administrator.");
                        }
                    }
                }
            }
            if (lb.getCommandList().isEmpty() == false && player.isOnline()) {
                for (CommandLoot cl : lb.getCommandList()) {
                    cl.execute((Player)player);
                }
            }
            if (lb.getMessageList().isEmpty() == false) {
                phatLootMessages.addAll(lb.getMessageList());
            }
            issuedReward = true;
        }
        if (rews.getExp() > 0 && player.isOnline()) {
            ((Player)player).giveExp(rews.getExp());
            issuedReward = true;
        }
        if (rews.getQuestPoints() > 0) {
            q.questPoints += rews.getQuestPoints();
            issuedReward = true;
        }
        if (rews.getCustomRewards().isEmpty() == false) {
            issuedReward = true;
        }
        
        // Inform player
        if (player.isOnline()) {
            Player p = (Player)player;
            p.sendMessage(ChatColor.GOLD + Lang.get(p, "questCompleteTitle").replace("<quest>", ChatColor.YELLOW + name
                    + ChatColor.GOLD));
            if (plugin.getSettings().canShowQuestTitles()) {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "title " + p.getName()
                        + " title " + "{\"text\":\"" + Lang.get(p, "quest") + " " + Lang.get(p, "complete") 
                        +  "\",\"color\":\"gold\"}");
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "title " + p.getName()
                        + " subtitle " + "{\"text\":\"" + name + "\",\"color\":\"yellow\"}");
            }
            p.sendMessage(ChatColor.GREEN + Lang.get(p, "questRewardsTitle"));
            if (!issuedReward) {
                p.sendMessage(ChatColor.GRAY + "- (" + Lang.get("none") + ")");
            } else if (!rews.getDetailsOverride().isEmpty()) {
                for (String s: rews.getDetailsOverride()) {
                    String message = ChatColor.DARK_GREEN + ConfigUtil.parseString(
                            ChatColor.translateAlternateColorCodes('&', s));
                    if (plugin.getDependencies().getPlaceholderApi() != null) {
                        message = PlaceholderAPI.setPlaceholders(p, message);
                    }
                    p.sendMessage("- " + message);
                }
            } else {
                if (rews.getQuestPoints() > 0) {
                    p.sendMessage("- " + ChatColor.DARK_GREEN + rews.getQuestPoints() + " " 
                            + Lang.get(p, "questPoints"));
                }
                for (ItemStack i : rews.getItems()) {
                    String text = "error";
                    if (i.hasItemMeta() && i.getItemMeta().hasDisplayName()) {
                        if (i.getEnchantments().isEmpty()) {
                            text = "- " + ChatColor.DARK_AQUA + ChatColor.ITALIC + i.getItemMeta().getDisplayName() 
                                    + ChatColor.RESET + ChatColor.GRAY + " x " + i.getAmount();
                        } else {
                            text = "- " + ChatColor.DARK_AQUA + ChatColor.ITALIC + i.getItemMeta().getDisplayName() 
                                    + ChatColor.RESET;            
                            try {
                                if (!i.getItemMeta().hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
                                    text +=  ChatColor.GRAY + " " + Lang.get(p, "with") + ChatColor.DARK_PURPLE;
                                    for (Entry<Enchantment, Integer> e : i.getEnchantments().entrySet()) {
                                        text += " " + ItemUtil.getPrettyEnchantmentName(e.getKey()) + ":" + e.getValue();
                                    }
                                }
                            } catch (Throwable tr) {
                                // Do nothing, hasItemFlag() not introduced until 1.8.6
                            }
                            text += ChatColor.GRAY + " x " + i.getAmount();
                        }
                    } else if (i.getDurability() != 0) {
                        if (i.getEnchantments().isEmpty()) {
                            text = "- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ":" + i.getDurability() + ChatColor.GRAY
                                    + " x " + i.getAmount();
                        } else {
                            text = "- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ":" + i.getDurability() + ChatColor.GRAY
                                    + " " + Lang.get(p, "with");
                            for (Entry<Enchantment, Integer> e : i.getEnchantments().entrySet()) {
                                text += " " + ItemUtil.getPrettyEnchantmentName(e.getKey()) + ":" + e.getValue();
                            }
                            text += ChatColor.GRAY + " x " + i.getAmount();
                        }
                    } else {
                        if (i.getEnchantments().isEmpty()) {
                            text = "- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ChatColor.GRAY + " x " + i.getAmount();
                        } else {
                            text = "- " + ChatColor.DARK_GREEN + ItemUtil.getName(i);
                            try {
                                if (!i.getItemMeta().hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
                                    text += ChatColor.GRAY + " " + Lang.get(p, "with");
                                    for (Entry<Enchantment, Integer> e : i.getEnchantments().entrySet()) {
                                        text += " " + ItemUtil.getPrettyEnchantmentName(e.getKey()) + ":" + e.getValue();
                                    }
                                }
                            } catch (Throwable tr) {
                                // Do nothing, hasItemFlag() not introduced until 1.8.6
                            }
                            text += ChatColor.GRAY + " x " + i.getAmount();
                        }
                    }
                    p.sendMessage(text);
                }
                for (ItemStack i : phatLootItems) {
                    if (i.hasItemMeta() && i.getItemMeta().hasDisplayName()) {
                        if (i.getEnchantments().isEmpty()) {
                            p.sendMessage("- " + ChatColor.DARK_AQUA + ChatColor.ITALIC + i.getItemMeta().getDisplayName()
                                    + ChatColor.RESET + ChatColor.GRAY + " x " + i.getAmount());
                        } else {
                            p.sendMessage("- " + ChatColor.DARK_AQUA + ChatColor.ITALIC + i.getItemMeta().getDisplayName()
                                    + ChatColor.RESET + ChatColor.GRAY + " x " + i.getAmount() + ChatColor.DARK_PURPLE + " " 
                                    + Lang.get(p, "enchantedItem"));
                        }
                    } else if (i.getDurability() != 0) {
                        if (i.getEnchantments().isEmpty()) {
                            p.sendMessage("- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ":" + i.getDurability() 
                                    + ChatColor.GRAY + " x " + i.getAmount());
                        } else {
                            p.sendMessage("- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ":" + i.getDurability() 
                                    + ChatColor.GRAY + " x " + i.getAmount() + ChatColor.DARK_PURPLE + " " 
                                    + Lang.get(p, "enchantedItem"));
                        }
                    } else {
                        if (i.getEnchantments().isEmpty()) {
                            p.sendMessage("- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ChatColor.GRAY + " x " 
                                    + i.getAmount());
                        } else {
                            p.sendMessage("- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ChatColor.GRAY + " x " 
                                    + i.getAmount() + ChatColor.DARK_PURPLE + " " + Lang.get(p, "enchantedItem"));
                        }
                    }
                }
                if (rews.getMoney() > 1) {
                    p.sendMessage("- " + ChatColor.DARK_GREEN + rews.getMoney() + " " + ChatColor.DARK_PURPLE 
                            + plugin.getDependencies().getCurrency(true));
                } else if (rews.getMoney() == 1) {
                    p.sendMessage("- " + ChatColor.DARK_GREEN + rews.getMoney() + " " + ChatColor.DARK_PURPLE 
                            + plugin.getDependencies().getCurrency(false));
                }
                if (rews.getExp() > 0 || phatLootExp > 0) {
                    int tot = rews.getExp() + phatLootExp;
                    p.sendMessage("- " + ChatColor.DARK_GREEN + tot + ChatColor.DARK_PURPLE + " " 
                            + Lang.get(p, "experience"));
                }
                if (rews.getCommands().isEmpty() == false) {
                    int index = 0;
                    for (String s : rews.getCommands()) {
                        if (rews.getCommandsOverrideDisplay().isEmpty() == false && rews.getCommandsOverrideDisplay().size() 
                                > index) {
                            if (!rews.getCommandsOverrideDisplay().get(index).trim().equals("")) {
                                p.sendMessage("- " + ChatColor.DARK_GREEN 
                                        + rews.getCommandsOverrideDisplay().get(index));
                            }
                        } else {
                            p.sendMessage("- " + ChatColor.DARK_GREEN + s);
                        }
                        index++;
                    }
                }
                if (rews.getPermissions().isEmpty() == false) {
                    int index = 0;
                    for (String s : rews.getPermissions()) {
                        if (rews.getPermissionWorlds() != null) {
                            p.sendMessage("- " + ChatColor.DARK_GREEN + s + " (" + rews.getPermissionWorlds().get(index)
                                    + ")");
                        } else {
                            p.sendMessage("- " + ChatColor.DARK_GREEN + s);
                            
                        }
                        index++;
                    }
                }
                if (rews.getMcmmoSkills().isEmpty() == false) {
                    for (String s : rews.getMcmmoSkills()) {
                        p.sendMessage("- " + ChatColor.DARK_GREEN 
                                + rews.getMcmmoAmounts().get(rews.getMcmmoSkills().indexOf(s)) + " " + ChatColor.DARK_PURPLE 
                                + s + " " + Lang.get(p, "experience"));
                    }
                }
                if (rews.getHeroesClasses().isEmpty() == false) {
                    for (String s : rews.getHeroesClasses()) {
                        p.sendMessage("- " + ChatColor.AQUA 
                                + rews.getHeroesAmounts().get(rews.getHeroesClasses().indexOf(s)) + " " + ChatColor.BLUE 
                                + s + " " + Lang.get(p, "experience"));
                    }
                }
                if (phatLootMessages.isEmpty() == false) {
                    for (String s : phatLootMessages) {
                        p.sendMessage("- " + s);
                    }
                }
                for (String s : rews.getCustomRewards().keySet()) {
                    CustomReward found = null;
                    for (CustomReward cr : plugin.getCustomRewards()) {
                        if (cr.getName().equalsIgnoreCase(s)) {
                            found = cr;
                            break;
                        }
                    }
                    if (found != null) {
                        Map<String, Object> datamap = rews.getCustomRewards().get(found.getName());
                        String message = found.getRewardName();
                        if (message != null) {
                            for (String key : datamap.keySet()) {
                                message = message.replace("%" + key + "%", datamap.get(key).toString());
                            }
                            p.sendMessage("- " + ChatColor.GOLD + message);
                        } else {
                            plugin.getLogger().warning("Failed to notify player: Custom Reward does not have an assigned name");
                        }
                        found.giveReward(p, rews.getCustomRewards().get(s));
                    } else {
                        plugin.getLogger().warning("Quester \"" + player.getName() + "\" completed the Quest \"" + name 
                                + "\", but the Custom Reward \"" + s + "\" could not be found. Does it still exist?");
                    }
                }
            }
        }
        q.saveData();
        if (player.isOnline()) {
            ((Player)player).updateInventory();
        }
        q.updateJournal();
        q.findCompassTarget();
        if (player.isOnline()) {
            QuesterPostCompleteQuestEvent postEvent = new QuesterPostCompleteQuestEvent(q, this);
            plugin.getServer().getPluginManager().callEvent(postEvent);
        }
        
        // Multiplayer
        if (opts.getShareProgressLevel() == 4) {
            List<Quester> mq = q.getMultiplayerQuesters(this);
            for (Quester qq : mq) {
                if (qq.getQuestData(this) != null) {
                    completeQuest(qq);
                }
            }
        }
    }
    
    /**
     * Force player to quit quest and inform them of their failure
     * 
     * @param quester The quester to be ejected
     */
    @SuppressWarnings("deprecation")
    public void failQuest(Quester quester) {
        QuesterPreFailQuestEvent preEvent = new QuesterPreFailQuestEvent(quester, this);
        plugin.getServer().getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) {
            return;
        }
        Player player = quester.getPlayer();
        String[] messages = {
                ChatColor.GOLD + Lang.get(player, "questObjectivesTitle").replace("<quest>", name),
                ChatColor.RED + Lang.get(player, "questFailed")
        };
        quester.quitQuest(this, messages);
        if (player.isOnline()) {
            player.updateInventory();
        }
        QuesterPostFailQuestEvent postEvent = new QuesterPostFailQuestEvent(quester, this);
        plugin.getServer().getPluginManager().callEvent(postEvent);
    }
    
    /**
     * Checks if quester is in WorldGuard region
     * 
     * @param quester The quester to check
     * @return true if quester is in region
     */
    public boolean isInRegion(Quester quester) {
        return isInRegion(quester.getPlayer());
    }

    /**
     * Checks if player is in WorldGuard region
     * 
     * @param player The player to check
     * @return true if player is in region
     */
    private boolean isInRegion(Player player) {
        if (regionStart == null) {
            return false;
        }
        if (plugin.getDependencies().getWorldGuardApi()
                .getApplicableRegionsIDs(player.getWorld(), player.getLocation()).contains(regionStart)) {
            return true;
        }
        return false;
    }
}
