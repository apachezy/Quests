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

package me.blackvein.quests.exceptions;

public class QuestFormatException extends Exception {

    private static final long serialVersionUID = -5960613170308750149L;
    private final String message;
    private final String questId;

    public QuestFormatException(String message, String questId) {
        super(message + ", see quest of ID " + questId);
        this.message = message + ", see quest of ID " + questId;
        this.questId = questId;
    }
    /**
     * Create a new instance of this class with the afflicted.
     * 
     * @deprecated Use {@link#QuestFormatException(String, String)}
     * @param quest The quest that an invalid value was set within.
     */
    public QuestFormatException(String questId) {
        this.message = "Failed to load quest of ID " + questId;
        this.questId = questId;
    }
    
    /**
     * Get the message associated with this exception.
     * 
     * @return The message.
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get the quest ID associated with this exception.
     * 
     * @return The quest that an invalid value was set within.
     */
    public String getQuestId() {
        return questId;
    }
}
