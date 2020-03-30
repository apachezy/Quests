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

package me.blackvein.quests.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import de.erethon.dungeonsxl.util.commons.chat.ChatColor;
import me.blackvein.quests.Quest;
import me.blackvein.quests.Quester;
import me.blackvein.quests.util.Lang;

public class ActionTimer extends BukkitRunnable {

    private Quester quester;
    private Quest quest;
    private int time;
    private boolean last;

    public ActionTimer(Quester quester, Quest quest, int time, boolean last) {
        this.quester = quester;
        this.quest = quest;
        this.time = time;
        this.last = last;
    }

    @Override
    public void run() {
        quester.removeTimer(getTaskId());
        if (last) {
            quest.failQuest(quester);
            quester.updateJournal();
        } else {
            quester.getPlayer().sendMessage(ChatColor.GREEN + Lang.get(quester.getPlayer(), "timerMessage")
                    .replace("<time>", ChatColor.RED + String.valueOf(time) + ChatColor.GREEN));
        }
    }
}
