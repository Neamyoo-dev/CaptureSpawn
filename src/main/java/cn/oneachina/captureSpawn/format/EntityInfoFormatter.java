package cn.oneachina.captureSpawn.format;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.DyeColor;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EntityInfoFormatter {
    public Component displayName(Entity entity) {
        Component customName = entity.customName();
        if (customName != null) {
            String plain = PlainTextComponentSerializer.plainText().serialize(customName);
            if (!plain.isBlank()) {
                return Component.text(plain);
            }
        }
        return Component.translatable(entity.getType().translationKey());
    }

    public List<Component> extraLoreLines(Entity entity) {
        List<Component> lines = new ArrayList<>(2);

        if (entity instanceof Villager villager) {
            Component line = Component.text()
                    .append(typeName(EntityType.VILLAGER))
                    .append(Component.text(" 职业: "))
                    .append(villagerProfessionName(villager.getProfession()))
                    .append(Component.text(" 等级: "))
                    .append(Component.text(villager.getVillagerLevel()))
                    .build();
            lines.add(line);
        } else if (entity instanceof Sheep sheep) {
            Component line = Component.text()
                    .append(typeName(EntityType.SHEEP))
                    .append(Component.text(" 颜色: "))
                    .append(dyeColorName(sheep.getColor()))
                    .build();
            lines.add(line);
        } else if (entity instanceof Cat cat) {
            Component line = Component.text()
                    .append(typeName(EntityType.CAT))
                    .append(Component.text(" 品种: "))
                    .append(catTypeName(cat.getCatType()))
                    .build();
            lines.add(line);
        } else if (entity instanceof Wolf wolf) {
            Component line = Component.text()
                    .append(typeName(EntityType.WOLF))
                    .append(Component.text(" 项圈: "))
                    .append(dyeColorName(wolf.getCollarColor()))
                    .append(Component.text(" 已驯服: "))
                    .append(Component.text(wolf.isTamed() ? "是" : "否"))
                    .build();
            lines.add(line);
        }

        return lines;
    }

    public Component typeName(EntityType type) {
        return Component.translatable(type.translationKey());
    }

    private static Component dyeColorName(DyeColor color) {
        if (color == null) {
            return Component.text("未知");
        }
        return Component.translatable("color.minecraft." + color.name().toLowerCase(Locale.ROOT));
    }

    private static Component villagerProfessionName(Villager.Profession profession) {
        if (profession == null) {
            return Component.text("未知");
        }
        return Component.translatable(profession.translationKey());
    }

    private static Component catTypeName(Cat.Type type) {
        if (type == null) {
            return Component.text("未知");
        }
        return Component.translatable(
                "cat_variant." + type.getKey().getNamespace() + "." + type.getKey().getKey()
        );
    }
}
