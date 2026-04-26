package cn.oneachina.captureSpawn.format;

import net.kyori.adventure.text.Component;
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
    public String displayName(Entity entity) {
        Component customName = entity.customName();
        if (customName != null) {
            String plain = PlainTextComponentSerializer.plainText().serialize(customName);
            if (plain != null && !plain.isBlank()) {
                return plain;
            }
        }
        return typeName(entity.getType());
    }

    public List<String> extraLoreLines(Entity entity) {
        List<String> lines = new ArrayList<>(2);
        if (entity instanceof Villager villager) {
            lines.add(typeName(EntityType.VILLAGER) + " 职业: " + villagerProfessionName(villager.getProfession()) + " 等级: " + villager.getVillagerLevel());
            return lines;
        }
        if (entity instanceof Sheep sheep) {
            lines.add(typeName(EntityType.SHEEP) + " 颜色: " + dyeColorName(sheep.getColor()));
            return lines;
        }
        if (entity instanceof Cat cat) {
            lines.add(typeName(EntityType.CAT) + " 品种: " + catTypeName(cat.getCatType()));
            return lines;
        }
        if (entity instanceof Wolf wolf) {
            lines.add(typeName(EntityType.WOLF) + " 项圈: " + dyeColorName(wolf.getCollarColor()) + " 已驯服: " + (wolf.isTamed() ? "是" : "否"));
            return lines;
        }
        return lines;
    }

    public String typeName(EntityType type) {
        if (type == EntityType.VILLAGER) {
            return "村民";
        }
        if (type == EntityType.SHEEP) {
            return "绵羊";
        }
        if (type == EntityType.CAT) {
            return "猫";
        }
        if (type == EntityType.WOLF) {
            return "狼";
        }
        return type.name().toLowerCase(Locale.ROOT);
    }

    private static String dyeColorName(DyeColor color) {
        if (color == null) {
            return "未知";
        }
        switch (color) {
            case WHITE:
                return "白色";
            case ORANGE:
                return "橙色";
            case MAGENTA:
                return "品红";
            case LIGHT_BLUE:
                return "淡蓝";
            case YELLOW:
                return "黄色";
            case LIME:
                return "黄绿";
            case PINK:
                return "粉色";
            case GRAY:
                return "灰色";
            case LIGHT_GRAY:
                return "淡灰";
            case CYAN:
                return "青色";
            case PURPLE:
                return "紫色";
            case BLUE:
                return "蓝色";
            case BROWN:
                return "棕色";
            case GREEN:
                return "绿色";
            case RED:
                return "红色";
            case BLACK:
                return "黑色";
        }
        return "未知";
    }

    private static String villagerProfessionName(Villager.Profession profession) {
        if (profession == null) {
            return "未知";
        }
        if (profession == Villager.Profession.NONE) {
            return "无";
        }
        if (profession == Villager.Profession.ARMORER) {
            return "盔甲匠";
        }
        if (profession == Villager.Profession.BUTCHER) {
            return "屠夫";
        }
        if (profession == Villager.Profession.CARTOGRAPHER) {
            return "制图师";
        }
        if (profession == Villager.Profession.CLERIC) {
            return "牧师";
        }
        if (profession == Villager.Profession.FARMER) {
            return "农民";
        }
        if (profession == Villager.Profession.FISHERMAN) {
            return "渔夫";
        }
        if (profession == Villager.Profession.FLETCHER) {
            return "制箭师";
        }
        if (profession == Villager.Profession.LEATHERWORKER) {
            return "皮匠";
        }
        if (profession == Villager.Profession.LIBRARIAN) {
            return "图书管理员";
        }
        if (profession == Villager.Profession.MASON) {
            return "石匠";
        }
        if (profession == Villager.Profession.NITWIT) {
            return "傻子";
        }
        if (profession == Villager.Profession.SHEPHERD) {
            return "牧羊人";
        }
        if (profession == Villager.Profession.TOOLSMITH) {
            return "工具匠";
        }
        if (profession == Villager.Profession.WEAPONSMITH) {
            return "武器匠";
        }
        return profession.name().toLowerCase(Locale.ROOT);
    }

    private static String catTypeName(Cat.Type type) {
        if (type == null) {
            return "未知";
        }
        if (type == Cat.Type.SIAMESE) {
            return "暹罗";
        }
        if (type == Cat.Type.TABBY) {
            return "虎斑";
        }
        if (type == Cat.Type.BLACK) {
            return "黑猫";
        }
        if (type == Cat.Type.RED) {
            return "红猫";
        }
        if (type == Cat.Type.CALICO) {
            return "三花";
        }
        if (type == Cat.Type.PERSIAN) {
            return "波斯";
        }
        if (type == Cat.Type.RAGDOLL) {
            return "布偶";
        }
        if (type == Cat.Type.WHITE) {
            return "白猫";
        }
        if (type == Cat.Type.JELLIE) {
            return "Jellie";
        }
        if (type == Cat.Type.ALL_BLACK) {
            return "全黑";
        }
        return type.name().toLowerCase(Locale.ROOT);
    }
}
