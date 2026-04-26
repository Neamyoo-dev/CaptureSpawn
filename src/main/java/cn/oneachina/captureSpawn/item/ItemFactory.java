package cn.oneachina.captureSpawn.item;

import cn.oneachina.captureSpawn.CaptureSpawn;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemFactory {
    private final CaptureSpawn plugin;
    private final Keys keys;
    private static final Pattern TEXTURE_URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    public ItemFactory(CaptureSpawn plugin, Keys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    public ItemStack createEmptyBall() {
        ItemStack item = createConfiguredItem("item.empty", BallData.empty(), null, List.of());
        if (item == null) {
            item = new ItemStack(Material.PLAYER_HEAD);
        }
        return item;
    }

    public ItemStack createFilledBall(String entityDisplay, List<String> extraLore, BallData data) {
        ItemStack item = createConfiguredItem("item.filled", data, entityDisplay, extraLore);
        if (item == null) {
            item = new ItemStack(Material.PLAYER_HEAD);
        }
        return item;
    }

    private ItemStack createConfiguredItem(String path, BallData data, String entityDisplay, List<String> extraLore) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            return null;
        }

        Material material = Material.PLAYER_HEAD;
        String mat = section.getString("material");
        if (mat != null) {
            Material parsed = Material.matchMaterial(mat);
            if (parsed != null) {
                material = parsed;
            }
        }

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        applyHeadTexture(meta, section.getConfigurationSection("head"));

        String name = section.getString("name");
        if (name != null && !name.isBlank()) {
            meta.setDisplayName(colorize(applyPlaceholders(name, entityDisplay, data)));
        }

        List<String> loreRaw = section.getStringList("lore");
        if (loreRaw != null && !loreRaw.isEmpty()) {
            List<String> lore = new ArrayList<>();
            for (String line : loreRaw) {
                String replaced = applyPlaceholders(line, entityDisplay, data);
                if ("{extra}".equals(replaced)) {
                    for (String extra : extraLore) {
                        lore.add(colorize(extra));
                    }
                    continue;
                }
                lore.add(colorize(replaced));
            }
            meta.setLore(lore);
        }

        boolean glow = section.getBoolean("glow", false);
        if (glow) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        int cmd = section.getInt("custom-model-data", 0);
        if (cmd > 0) {
            meta.setCustomModelData(cmd);
        }

        meta.getPersistentDataContainer().set(keys.ball, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(keys.captured, PersistentDataType.BYTE, (byte) (data.captured() ? 1 : 0));
        meta.getPersistentDataContainer().set(keys.formatVersion, PersistentDataType.INTEGER, 1);
        meta.getPersistentDataContainer().set(keys.uid, PersistentDataType.STRING, UUID.randomUUID().toString());
        if (data.captured()) {
            if (data.entityType() != null) {
                meta.getPersistentDataContainer().set(keys.entityType, PersistentDataType.STRING, data.entityType());
            }
            if (data.entityNbt() != null) {
                meta.getPersistentDataContainer().set(keys.entityNbt, PersistentDataType.STRING, data.entityNbt());
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    private static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private static String applyPlaceholders(String input, String entityDisplay, BallData data) {
        String out = input;
        if (entityDisplay != null) {
            out = out.replace("{entity_display}", entityDisplay);
        }
        if (data.entityType() != null) {
            out = out.replace("{entity_type}", data.entityType());
        }
        return out;
    }

    private void applyHeadTexture(ItemMeta meta, ConfigurationSection headSection) {
        if (!(meta instanceof SkullMeta skullMeta)) {
            return;
        }
        if (headSection == null) {
            return;
        }

        String url = headSection.getString("url");
        if (url == null || url.isBlank()) {
            String texture = headSection.getString("texture");
            url = extractTextureUrl(texture);
        }
        if (url == null || url.isBlank()) {
            return;
        }

        PlayerProfile profile = plugin.getServer().createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        try {
            textures.setSkin(new java.net.URL(url));
        } catch (Exception ignored) {
            return;
        }
        profile.setTextures(textures);
        skullMeta.setOwnerProfile(profile);
    }

    private static String extractTextureUrl(String base64Texture) {
        if (base64Texture == null || base64Texture.isBlank()) {
            return null;
        }
        String json;
        try {
            json = new String(java.util.Base64.getDecoder().decode(base64Texture), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            json = base64Texture;
        }
        Matcher matcher = TEXTURE_URL.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }
}
