/**
 * ============================================================
 * [HoneyReservoirBlockEntity.java]
 * Description: Interface de délégation de fluide vers un contrôleur multibloc
 * ============================================================
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    ⚠️⚠️⚠️ RÈGLE ABSOLUE CRITIQUE ⚠️⚠️⚠️                    ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║   LE HONEY RESERVOIR NE DOIT **JAMAIS** STOCKER DE FLUIDE LOCALEMENT    ║
 * ║                                                                          ║
 * ║   LE HONEY RESERVOIR NE DOIT **JAMAIS** STOCKER DE FLUIDE LOCALEMENT    ║
 * ║                                                                          ║
 * ║   LE HONEY RESERVOIR NE DOIT **JAMAIS** STOCKER DE FLUIDE LOCALEMENT    ║
 * ║                                                                          ║
 * ║   LE HONEY RESERVOIR NE DOIT **JAMAIS** STOCKER DE FLUIDE LOCALEMENT    ║
 * ║                                                                          ║
 * ║   LE HONEY RESERVOIR NE DOIT **JAMAIS** STOCKER DE FLUIDE LOCALEMENT    ║
 * ║                                                                          ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║   Ce bloc est un PROXY PUR vers le contrôleur multibloc.                ║
 * ║   - Quand formé: délègue TOUT au contrôleur via getFluidHandlerForBlock ║
 * ║   - Quand non-formé: N'EXPOSE AUCUNE CAPABILITY (retourne null)         ║
 * ║   - Le contrôleur possède TOUS les tanks                                 ║
 * ║   - Ce bloc ne fait que rediriger les requêtes                          ║
 * ║                                                                          ║
 * ║   VIOLATION DE CETTE RÈGLE = BUG CRITIQUE                               ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities            | Type registration    | super()                        |
 * | MultiblockController          | Multiblock check     | isPartOfFormedMultiblock       |
 * | MultiblockCapabilityProvider  | Délégation caps      | findCapabilityProvider         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyReservoirBlock.java
 * - Apica.java (capability delegation lambdas)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.altar;

import com.chapeau.apica.core.multiblock.MultiblockCapabilityProvider;
import com.chapeau.apica.core.multiblock.MultiblockController;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   PROXY PUR - AUCUN STOCKAGE LOCAL - DÉLÉGATION UNIQUEMENT              ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * Interface visuelle et de capability vers un contrôleur multibloc.
 * NE STOCKE JAMAIS de fluide - délègue tout au contrôleur.
 */
public class HoneyReservoirBlockEntity extends BlockEntity {

    /**
     * ╔══════════════════════════════════════════════════════════════════════╗
     * ║   ⚠️⚠️⚠️ AUCUN TANK LOCAL - INTERDIT - VOIR EN-TÊTE ⚠️⚠️⚠️           ║
     * ║                                                                      ║
     * ║   NE JAMAIS AJOUTER de FluidTank ici.                               ║
     * ║   Le stockage est EXCLUSIVEMENT dans le contrôleur multibloc.       ║
     * ║   Ce bloc est un PROXY, pas un conteneur.                           ║
     * ║                                                                      ║
     * ║   Les champs visualFluid* ci-dessous sont un CACHE DE RENDU,        ║
     * ║   PAS un stockage. Ils sont mis à jour par le contrôleur pour       ║
     * ║   l'affichage client-side uniquement.                               ║
     * ╚══════════════════════════════════════════════════════════════════════╝
     */

    /**
     * Capacité VISUELLE pour le calcul du fillRatio dans le renderer.
     * Ce n'est PAS une capacité de stockage - juste une référence pour l'affichage.
     */
    public static final int VISUAL_CAPACITY = 4000;

    // Position du contrôleur multibloc (pour délégation de capabilities)
    @Nullable
    private BlockPos controllerPos = null;

    // Spread offset pour le multibloc Storage Controller (en blocs, coordonnées monde)
    private float formedSpreadX = 0.0f;
    private float formedSpreadZ = 0.0f;

    /**
     * ⚠️ CACHE VISUEL UNIQUEMENT - PAS UN STOCKAGE ⚠️
     * Ces champs sont synchronisés par le contrôleur pour le rendu client.
     * Le fluide "réel" est dans le contrôleur, pas ici.
     */
    private net.neoforged.neoforge.fluids.FluidStack visualFluid = net.neoforged.neoforge.fluids.FluidStack.EMPTY;
    private float visualFillRatio = 0f;

    public HoneyReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.HONEY_RESERVOIR.get(), pos, state);
        // ⚠️ PAS DE TANK - PROXY UNIQUEMENT ⚠️
    }

    public void serverTick() {
        // Pas de logique tick - proxy pur
    }

    // ==================== Cache visuel (rendu uniquement) ====================

    /**
     * ⚠️ APPELÉ PAR LE CONTRÔLEUR UNIQUEMENT ⚠️
     * Met à jour le cache visuel pour le rendu client.
     * Ce n'est PAS un stockage fonctionnel.
     */
    public void setVisualFluid(net.neoforged.neoforge.fluids.FluidStack fluid, float fillRatio) {
        this.visualFluid = fluid.copy();
        this.visualFillRatio = fillRatio;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Pour le renderer uniquement - retourne le cache visuel.
     */
    public net.neoforged.neoforge.fluids.FluidStack getVisualFluid() {
        return visualFluid;
    }

    /**
     * Pour le renderer uniquement - retourne le ratio de remplissage visuel.
     */
    public float getVisualFillRatio() {
        return visualFillRatio;
    }

    // ==================== Controller delegation ====================

    public void setControllerPos(@Nullable BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Set le controllerPos sans déclencher invalidateCapabilities.
     * Utilisé par le contrôleur multibloc qui fait l'invalidation globale après coup.
     */
    public void setControllerPosQuiet(@Nullable BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    /**
     * Cherche le MultiblockCapabilityProvider du contrôleur associé.
     * Retourne null si pas de contrôleur, pas formé, ou pas un provider.
     */
    @Nullable
    public MultiblockCapabilityProvider findCapabilityProvider() {
        if (controllerPos == null || level == null) return null;
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof MultiblockCapabilityProvider provider
                && be instanceof MultiblockController controller
                && controller.isFormed()) {
            return provider;
        }
        return null;
    }

    /**
     * Vérifie si ce réservoir fait partie d'un multiblock formé.
     */
    public boolean isPartOfFormedMultiblock() {
        if (controllerPos == null || level == null) return false;
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof MultiblockController controller) {
            return controller.isFormed();
        }
        return false;
    }

    // ==================== Spread (Storage Controller multibloc) ====================

    public float getFormedSpreadX() {
        return formedSpreadX;
    }

    public float getFormedSpreadZ() {
        return formedSpreadZ;
    }

    public void setFormedSpread(float spreadX, float spreadZ) {
        this.formedSpreadX = spreadX;
        this.formedSpreadZ = spreadZ;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // ⚠️ PAS DE SAUVEGARDE DE FLUIDE FONCTIONNEL - PROXY UNIQUEMENT ⚠️
        // Le cache visuel n'est PAS sauvegardé - il sera re-synchonisé par le contrôleur
        if (controllerPos != null) {
            tag.putLong("ControllerPos", controllerPos.asLong());
        }
        if (formedSpreadX != 0.0f) {
            tag.putFloat("FormedSpreadX", formedSpreadX);
        }
        if (formedSpreadZ != 0.0f) {
            tag.putFloat("FormedSpreadZ", formedSpreadZ);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // ⚠️ PAS DE CHARGEMENT DE FLUIDE FONCTIONNEL - PROXY UNIQUEMENT ⚠️
        controllerPos = tag.contains("ControllerPos") ? BlockPos.of(tag.getLong("ControllerPos")) : null;
        formedSpreadX = tag.getFloat("FormedSpreadX");
        formedSpreadZ = tag.getFloat("FormedSpreadZ");
        // visualFluid sera re-sync par le contrôleur au prochain tick
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (controllerPos != null) {
            tag.putLong("ControllerPos", controllerPos.asLong());
        }
        if (formedSpreadX != 0.0f) {
            tag.putFloat("FormedSpreadX", formedSpreadX);
        }
        if (formedSpreadZ != 0.0f) {
            tag.putFloat("FormedSpreadZ", formedSpreadZ);
        }
        // Cache visuel pour le rendu client-side
        if (!visualFluid.isEmpty()) {
            tag.put("VisualFluid", visualFluid.save(registries));
            tag.putFloat("VisualFillRatio", visualFillRatio);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        controllerPos = tag.contains("ControllerPos") ? BlockPos.of(tag.getLong("ControllerPos")) : null;
        formedSpreadX = tag.getFloat("FormedSpreadX");
        formedSpreadZ = tag.getFloat("FormedSpreadZ");
        // Cache visuel pour le rendu
        if (tag.contains("VisualFluid")) {
            visualFluid = net.neoforged.neoforge.fluids.FluidStack.parseOptional(registries, tag.getCompound("VisualFluid"));
            visualFillRatio = tag.getFloat("VisualFillRatio");
        } else {
            visualFluid = net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            visualFillRatio = 0f;
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
