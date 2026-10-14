package com.net.wenwen2;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
public final class TickConfig {
    public enum Scope { ALL_NON_PLAYERS, MOBS_ONLY }
    public enum DistanceMode { SPHERE, CYLINDER }
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, PROTECT_BOSSES, PROTECT_NAMED, PROTECT_TAMED, FREEZE_WITHOUT_PLAYERS;
    public static final ForgeConfigSpec.DoubleValue DISTANCE;
    public static final ForgeConfigSpec.DoubleValue HORIZONTAL_DISTANCE, VERTICAL_DISTANCE;
    public static final ForgeConfigSpec.EnumValue<DistanceMode> DISTANCE_MODE;
    public static final ForgeConfigSpec.BooleanValue PROTECT_DYING;
    public static final ForgeConfigSpec.BooleanValue PROTECT_PROJECTILES;
    public static final ForgeConfigSpec.BooleanValue DELAY_FROZEN_CHECKS;
    public static final ForgeConfigSpec.IntValue FROZEN_CHECK_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue HIDE_DISTANT_ENTITIES, FREEZE_CLIENT_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue DISABLE_FROZEN_PUSHING, LIMIT_NATURAL_SPAWNS, MAINTAIN_NATURAL_DESPAWN;
    public static final ForgeConfigSpec.EnumValue<Scope> SCOPE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ENABLED = b.comment("是否启用距离优化。关闭后停 tick、隐藏、推挤限制和自然刷怪限制全部停用。").define("enabled", true);
        HIDE_DISTANT_ENTITIES = b.comment("客户端是否隐藏距离外的实体，包括模型、阴影、名称和火焰绘制。",
            "使用与服务端 tick 相同的距离参数；玩家、白名单和受保护实体仍显示。声音和独立粒子不受此项控制。")
            .define("hideDistantEntities", true);
        FREEZE_CLIENT_ENTITIES = b.comment("客户端是否暂停距离外实体的 tick，减少本地动画和更新开销。",
            "恢复时重置插值起点；网络同步仍正常接收。模组实体若恢复异常可单独关闭此项。")
            .define("freezeClientEntities", true);
        DISABLE_FROZEN_PUSHING = b.comment("是否禁止被冻结实体参与实体间推挤。默认关闭；开启可能改变运输和实体重叠行为。",
            "不设置 noPhysics，不改变方块碰撞、攻击命中和弹射物命中。模组自定义推挤逻辑可能不受此项控制。")
            .define("disableFrozenPushing", false);
        LIMIT_NATURAL_SPAWNS = b.comment("是否阻止距离外的自然刷怪。默认关闭，会改变刷怪分布及农场效率。",
            "只拦截 NATURAL，不拦截区块生成、存档加载、刷怪笼、繁殖、指令和物品生成；白名单及保护条件仍有效。")
            .define("limitNaturalSpawns", false);
        MAINTAIN_NATURAL_DESPAWN = b.comment("冻结时是否继续增加非持久化、启用 AI 生物的空闲计时。默认开启。",
            "自然消失仍交给原版和 Forge 在实体 tick 前的检查，不额外强制删除实体。")
            .define("maintainNaturalDespawn", true);
        DELAY_FROZEN_CHECKS = b.comment("是否减少已冻结实体的距离检查频率。默认开启，关闭时每 tick 检查并立即恢复。",
            "开启后，玩家靠近时可能延迟最多 frozenCheckInterval 个服务端 tick 恢复。",
            "死亡、骑乘、传送和其他保护条件仍每 tick 检查；配置或标签重载后立即重新判断。")
            .define("delayFrozenChecks", true);
        FROZEN_CHECK_INTERVAL = b.comment("仅 delayFrozenChecks 开启时使用：已冻结实体重新检查距离的间隔。",
            "默认 5 tick；正常 20 TPS 下约 0.25 秒。正常运行的实体仍每 tick 检查。")
            .defineInRange("frozenCheckInterval", 5, 1, 200);
        DISTANCE_MODE = b.comment("距离模式：SPHERE 为三维球形，使用 activationDistance，默认保持原有行为。",
            "CYLINDER 为圆柱形，同时检查水平半径和垂直高度差；必须对同一位玩家满足两个条件。")
            .defineEnum("distanceMode", DistanceMode.SPHERE);
        DISTANCE = b.comment("实体到同维度最近非旁观玩家的三维距离，单位：方块。",
            "距离大于此值时暂停 tick；小于或等于此值时正常运行。默认：64 格。")
            .defineInRange("activationDistance", 64.0, 1.0, 32768.0);
        HORIZONTAL_DISTANCE = b.comment("仅 CYLINDER 模式使用：水平半径，单位方块，边界内正常 tick。")
            .defineInRange("horizontalDistance", 64.0, 1.0, 32768.0);
        VERTICAL_DISTANCE = b.comment("仅 CYLINDER 模式使用：最大垂直高度差，单位方块，边界内正常 tick。")
            .defineInRange("verticalDistance", 32.0, 1.0, 32768.0);
        PROTECT_DYING = b.comment("是否让死亡过程中的生物继续 tick，确保死亡动画和实体清理完成。建议开启。")
            .define("protectDying", true);
        PROTECT_PROJECTILES = b.comment("是否让弹射物始终正常 tick 并保留显示，避免远处箭、火球、投掷物等卡在空中。默认开启。",
            "同时保护客户端和服务端；包括继承原版 Projectile 的模组弹射物。未继承此类的模组弹射物请加入白名单。",
            "不强制加载区块；离开原版实体模拟/网络追踪范围时，仍遵循原版规则。")
            .define("protectProjectiles", true);
        SCOPE = b.comment("停 tick 的实体范围。",
            "ALL_NON_PLAYERS：所有非玩家实体，包括生物、掉落物、弹射物、矿车和模组实体。",
            "MOBS_ONLY：只处理 Mob 类生物。玩家始终不受影响。")
            .defineEnum("scope", Scope.ALL_NON_PLAYERS);
        PROTECT_BOSSES = b.comment("是否保护不能切换维度的实体，包含原版末影龙和凋灵。",
            "模组 Boss 不一定符合此条件，建议另外加入白名单。")
            .define("protectBosses", true);
        PROTECT_NAMED = b.comment("是否让有自定义名称的实体始终正常 tick。默认关闭。")
            .define("protectNamed", false);
        PROTECT_TAMED = b.comment("是否让已驯服的动物和马始终正常 tick。默认开启。")
            .define("protectTamed", true);
        FREEZE_WITHOUT_PLAYERS = b.comment("维度中没有非旁观玩家时，是否暂停符合条件的实体。",
            "默认开启；白名单实体和受保护实体仍然正常运行。")
            .define("freezeWithoutPlayers", true);
        WHITELIST = b.comment("白名单：匹配的实体不受距离限制，始终正常 tick。支持以下三种格式：",
            "实体 ID，例如 minecraft:villager。",
            "整个模组，例如 create:*。",
            "实体类型标签，例如 #minecraft:raiders。标签随数据包重载更新。",
            "默认保护原版末影龙和凋灵。可设置为空列表 []。",
            "远处实体机器、运输矿车等也会暂停，请按整合包玩法添加白名单。")
            .defineListAllowEmpty("whitelist", List.of("minecraft:ender_dragon", "minecraft:wither"), TickConfig::validEntry);
        SPEC = b.build();
    }
    private static boolean validEntry(Object value) {
        if (!(value instanceof String s) || s.isBlank()) return false;
        if (s.startsWith("#")) return ResourceLocation.tryParse(s.substring(1)) != null;
        if (s.endsWith(":*")) return s.length() > 2 && ResourceLocation.tryParse(s.substring(0, s.length()-1) + "placeholder") != null;
        return s.contains(":") && ResourceLocation.tryParse(s) != null;
    }
    private TickConfig() {}
}
