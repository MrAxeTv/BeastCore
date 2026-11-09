# BeastLib 1.7.4 - GUI Compatibility Fix

This update fixes GUI errors affecting BeastTokens and other plugins compiled against older BeastLib versions.

- Restores the legacy **Player** methods alongside the current **HumanEntity** methods for opening and closing menus, including paginated, scrolling and storage menus.
- Fixes the **NoSuchMethodError: Gui.open(Player)** that could prevent **/tokenshop** from opening.
- Preserves normal menu behavior and page/close arguments. Existing compiled plugins can continue using their original GUI calls.
- Includes the same compatibility methods in the server plugin and developer API artifact.

**Updating:** Stop the server, replace the old BeastLib jar with **BeastLib 1.7.4**, then start the server. Keep only one BeastLib jar installed. No configuration reset is needed.

For the matching BeastTokens release, install **BeastTokens 4.3.15** during the same restart.
