package cn.oneachina.captureSpawn.item;

import cn.oneachina.captureSpawn.CaptureSpawn;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.EntityType;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemFactory {
    private final CaptureSpawn plugin;
    private final Keys keys;
    private static final Pattern TEXTURE_URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

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

    public ItemStack createFilledBall(Component entityDisplay, List<Component> extraLore, BallData data) {
        ItemStack item = createConfiguredItem("item.filled", data, entityDisplay, extraLore);
        if (item == null) {
            item = new ItemStack(Material.PLAYER_HEAD);
        }
        return item;
    }

    private ItemStack createConfiguredItem(String path, BallData data, Component entityDisplay, List<Component> extraLore) {
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
            meta.displayName(renderTemplate(name, entityDisplay, entityTypeComponent(data)));
        }

        List<String> loreRaw = section.getStringList("lore");
        if (!loreRaw.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreRaw) {
                if (line != null && line.trim().equals("{extra}")) {
                    lore.addAll(extraLore);
                    continue;
                }
                lore.add(renderTemplate(line, entityDisplay, entityTypeComponent(data)));
            }
            meta.lore(lore);
        }

        boolean glow = section.getBoolean("glow", false);
        if (glow) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        int cmd = section.getInt("custom-model-data", 0);
        if (cmd > 0) {
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setFloats(List.of((float) cmd));
            meta.setCustomModelDataComponent(component);
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

    private static Component entityTypeComponent(BallData data) {
        if (data == null || data.entityType() == null || data.entityType().isBlank()) {
            return Component.empty();
        }
        try {
            EntityType type = EntityType.valueOf(data.entityType().trim().toUpperCase(Locale.ROOT));
            return Component.translatable(type.translationKey());
        } catch (Exception ex) {
            return Component.text(data.entityType());
        }
    }

    private static Component renderTemplate(String template, Component entityDisplay, Component entityType) {
        if (template == null) {
            return Component.empty();
        }
        Component display = entityDisplay == null ? Component.empty() : entityDisplay;
        Component type = entityType == null ? Component.empty() : entityType;

        int idx = 0;
        Component out = Component.empty();
        while (idx < template.length()) {
            int nextDisplay = template.indexOf("{entity_display}", idx);
            int nextType = template.indexOf("{entity_type}", idx);
            int next = -1;
            boolean useDisplay = false;
            if (nextDisplay != -1 && (nextType == -1 || nextDisplay < nextType)) {
                next = nextDisplay;
                useDisplay = true;
            } else if (nextType != -1) {
                next = nextType;
            }
            if (next == -1) {
                out = out.append(LEGACY.deserialize(template.substring(idx)));
                break;
            }
            if (next > idx) {
                out = out.append(LEGACY.deserialize(template.substring(idx, next)));
            }
            out = out.append(useDisplay ? display : type);
            idx = next + (useDisplay ? "{entity_display}".length() : "{entity_type}".length());
        }

        if (out instanceof TextComponent tc) {
            String plain = PlainTextComponentSerializer.plainText().serialize(tc);
            if (plain.isBlank()) {
                return Component.empty();
            }
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

        PlayerProfile profile = plugin.getServer().createProfile(UUID.randomUUID(), null);
        PlayerTextures textures = profile.getTextures();
        try {
            textures.setSkin(URI.create(url).toURL());
        } catch (Exception ignored) {
            return;
        }
        profile.setTextures(textures);
        skullMeta.setPlayerProfile(profile);
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
