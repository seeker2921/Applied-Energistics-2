package appeng.core.sync;

import java.lang.reflect.Constructor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.exceptions.AppEngException;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.INetworkTool;
import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IStorageMonitorable;
import appeng.client.gui.GuiNull;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerNull;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCellWorkbench;
import appeng.container.implementations.ContainerChest;
import appeng.container.implementations.ContainerCondenser;
import appeng.container.implementations.ContainerDrive;
import appeng.container.implementations.ContainerGrinder;
import appeng.container.implementations.ContainerIOPort;
import appeng.container.implementations.ContainerInterface;
import appeng.container.implementations.ContainerLevelEmitter;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.implementations.ContainerMEPortableCell;
import appeng.container.implementations.ContainerNetworkStatus;
import appeng.container.implementations.ContainerNetworkTool;
import appeng.container.implementations.ContainerPriority;
import appeng.container.implementations.ContainerQNB;
import appeng.container.implementations.ContainerSecurity;
import appeng.container.implementations.ContainerStorageBus;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.implementations.ContainerVibrationChamber;
import appeng.container.implementations.ContainerWireless;
import appeng.container.implementations.ContainerWirelessTerm;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.IPriorityHost;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.parts.automation.PartLevelEmitter;
import appeng.parts.misc.PartStorageBus;
import appeng.server.AccessType;
import appeng.tile.grindstone.TileGrinder;
import appeng.tile.misc.TileCellWorkbench;
import appeng.tile.misc.TileCondenser;
import appeng.tile.misc.TileSecurity;
import appeng.tile.misc.TileVibrationChamber;
import appeng.tile.networking.TileWireless;
import appeng.tile.qnb.TileQuantumBridge;
import appeng.tile.storage.TileChest;
import appeng.tile.storage.TileDrive;
import appeng.tile.storage.TileIOPort;
import appeng.util.Platform;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.ReflectionHelper;

public enum GuiBridge implements IGuiHandler
{
	GUI_Handler(),

	GUI_GRINDER(ContainerGrinder.class, TileGrinder.class, false, null),

	GUI_QNB(ContainerQNB.class, TileQuantumBridge.class, false, SecurityPermissions.BUILD),

	GUI_CHEST(ContainerChest.class, TileChest.class, false, SecurityPermissions.BUILD),

	GUI_WIRELESS(ContainerWireless.class, TileWireless.class, false, SecurityPermissions.BUILD),

	GUI_ME(ContainerMEMonitorable.class, IStorageMonitorable.class, false, null),

	GUI_PORTABLE_CELL(ContainerMEPortableCell.class, IPortableCell.class, true, null),

	GUI_WIRELESS_TERM(ContainerWirelessTerm.class, WirelessTerminalGuiObject.class, true, null),

	GUI_NETWORK_STATUS(ContainerNetworkStatus.class, INetworkTool.class, true, null),

	GUI_NETWORK_TOOL(ContainerNetworkTool.class, INetworkTool.class, true, null),

	GUI_DRIVE(ContainerDrive.class, TileDrive.class, false, SecurityPermissions.BUILD),

	GUI_VIBRATIONCHAMBER(ContainerVibrationChamber.class, TileVibrationChamber.class, false, null),

	GUI_CONDENSER(ContainerCondenser.class, TileCondenser.class, false, null),

	GUI_INTERFACE(ContainerInterface.class, IInterfaceHost.class, false, SecurityPermissions.BUILD),

	GUI_BUS(ContainerUpgradeable.class, IUpgradeableHost.class, false, SecurityPermissions.BUILD),

	GUI_IOPORT(ContainerIOPort.class, TileIOPort.class, false, SecurityPermissions.BUILD),

	GUI_STORAGEBUS(ContainerStorageBus.class, PartStorageBus.class, false, SecurityPermissions.BUILD),

	GUI_PRIORITY(ContainerPriority.class, IPriorityHost.class, false, SecurityPermissions.BUILD),

	GUI_SECURITY(ContainerSecurity.class, TileSecurity.class, false, SecurityPermissions.SECURITY),

	// extends (Container/Gui) + Bus
	GUI_LEVELEMITTER(ContainerLevelEmitter.class, PartLevelEmitter.class, false, SecurityPermissions.BUILD),

	GUI_CELLWORKBENCH(ContainerCellWorkbench.class, TileCellWorkbench.class, false, null);

	private Class Tile;
	private Class Gui;
	private Class Container;
	private boolean isItem;
	private SecurityPermissions requiredPermission;

	private GuiBridge() {
		Tile = null;
		Gui = null;
		Container = null;
	}

	/**
	 * I honestly wish I could just use the GuiClass Names myself, but I can't access them without MC's Server
	 * Exploding.
	 */
	private void getGui()
	{
		if ( Platform.isClient() )
		{
			String start = Container.getName();
			String GuiClass = start.replaceFirst( "container.", "client.gui." ).replace( ".Container", ".Gui" );
			if ( start.equals( GuiClass ) )
				throw new RuntimeException( "Unable to find gui class" );
			Gui = ReflectionHelper.getClass( this.getClass().getClassLoader(), GuiClass );
			if ( Gui == null )
				throw new RuntimeException( "Cannot Load class: " + GuiClass );
		}
	}

	private GuiBridge(Class _Container, SecurityPermissions requiredPermission) {
		this.requiredPermission = requiredPermission;
		Container = _Container;
		Tile = null;
		getGui();
	}

	private GuiBridge(Class _Container, Class _Tile, boolean isItem, SecurityPermissions requiredPermission) {
		this.requiredPermission = requiredPermission;
		Container = _Container;
		this.isItem = isItem;
		Tile = _Tile;
		getGui();
	}

	public boolean CorrectTileOrPart(Object tE)
	{
		if ( Tile == null )
			throw new RuntimeException( "This Gui Cannot use the standard Handler." );

		return Tile.isInstance( tE );
	}

	public Object ConstructContainer(InventoryPlayer inventory, ForgeDirection side, Object tE)
	{
		try
		{
			Constructor[] c = Container.getConstructors();
			if ( c.length == 0 )
				throw new AppEngException( "Invalid Gui Class" );
			return c[0].newInstance( inventory, tE );
		}
		catch (Throwable t)
		{
			throw new RuntimeException( t );
		}
	}

	public Object ConstructGui(InventoryPlayer inventory, ForgeDirection side, Object tE)
	{
		try
		{
			Constructor[] c = Gui.getConstructors();
			if ( c.length == 0 )
				throw new AppEngException( "Invalid Gui Class" );
			return c[0].newInstance( inventory, tE );
		}
		catch (Throwable t)
		{
			throw new RuntimeException( t );
		}
	}

	private Object updateGui(Object newContainer, World w, int x, int y, int z, ForgeDirection side)
	{
		if ( newContainer instanceof AEBaseContainer )
		{
			AEBaseContainer bc = (AEBaseContainer) newContainer;
			bc.openContext = new ContainerOpenContext();
			bc.openContext.w = w;
			bc.openContext.x = x;
			bc.openContext.y = y;
			bc.openContext.z = z;
			bc.openContext.side = side;
		}

		return newContainer;
	}

	@Override
	public Object getServerGuiElement(int ID_ORDINAL, EntityPlayer player, World w, int x, int y, int z)
	{
		ForgeDirection side = ForgeDirection.getOrientation( ID_ORDINAL & 0x07 );
		GuiBridge ID = values()[ID_ORDINAL >> 3];

		if ( ID.isItem() )
		{
			ItemStack it = player.inventory.getCurrentItem();
			Object myItem = getGuiObject( it, player, w, x, y, z );
			if ( myItem != null && ID.CorrectTileOrPart( myItem ) )
				return updateGui( ID.ConstructContainer( player.inventory, side, myItem ), w, x, y, z, side );
		}
		else
		{
			TileEntity TE = w.getBlockTileEntity( x, y, z );
			if ( TE instanceof IPartHost )
			{
				((IPartHost) TE).getPart( side );
				IPart part = ((IPartHost) TE).getPart( side );
				if ( ID.CorrectTileOrPart( part ) )
					return updateGui( ID.ConstructContainer( player.inventory, side, part ), w, x, y, z, side );
			}
			else
			{
				if ( ID.CorrectTileOrPart( TE ) )
					return updateGui( ID.ConstructContainer( player.inventory, side, TE ), w, x, y, z, side );
			}
		}

		return new ContainerNull();
	}

	private Object getGuiObject(ItemStack it, EntityPlayer player, World w, int x, int y, int z)
	{
		if ( it != null )
		{
			if ( it.getItem() instanceof IGuiItem )
			{
				return ((IGuiItem) it.getItem()).getGuiObject( it, w, x, y, z );
			}

			IWirelessTermHandler wh = AEApi.instance().registries().wireless().getWirelessTerminalHandler( it );
			if ( wh != null )
				return new WirelessTerminalGuiObject( wh, it, player, w, x, y, z );
		}

		return null;
	}

	public boolean isItem()
	{
		return isItem;
	}

	@Override
	public Object getClientGuiElement(int ID_ORDINAL, EntityPlayer player, World w, int x, int y, int z)
	{
		ForgeDirection side = ForgeDirection.getOrientation( ID_ORDINAL & 0x07 );
		GuiBridge ID = values()[ID_ORDINAL >> 3];

		if ( ID.isItem() )
		{
			ItemStack it = player.inventory.getCurrentItem();
			Object myItem = getGuiObject( it, player, w, x, y, z );
			if ( ID.CorrectTileOrPart( myItem ) )
				return ID.ConstructGui( player.inventory, side, myItem );
		}
		else
		{
			TileEntity TE = w.getBlockTileEntity( x, y, z );

			if ( TE instanceof IPartHost )
			{
				((IPartHost) TE).getPart( side );
				IPart part = ((IPartHost) TE).getPart( side );
				if ( ID.CorrectTileOrPart( part ) )
					return ID.ConstructGui( player.inventory, side, part );
			}
			else
			{
				if ( ID.CorrectTileOrPart( TE ) )
					return ID.ConstructGui( player.inventory, side, TE );
			}
		}

		return new GuiNull( new ContainerNull() );
	}

	public boolean hasPermissions(TileEntity te, int x, int y, int z, ForgeDirection side, EntityPlayer player)
	{
		World w = player.getEntityWorld();

		if ( Platform.hasPermissions( x, y, z, player, AccessType.BLOCK_ACCESS ) )
		{
			if ( isItem() )
			{
				ItemStack it = player.inventory.getCurrentItem();
				if ( it != null && it.getItem() instanceof IGuiItem )
				{
					Object myItem = ((IGuiItem) it.getItem()).getGuiObject( it, w, x, y, z );
					if ( CorrectTileOrPart( myItem ) )
					{
						return true;
					}
				}
			}
			else
			{
				TileEntity TE = w.getBlockTileEntity( x, y, z );
				if ( TE instanceof IPartHost )
				{
					((IPartHost) TE).getPart( side );
					IPart part = ((IPartHost) TE).getPart( side );
					if ( CorrectTileOrPart( part ) )
						return securityCheck( part, player );
				}
				else
				{
					if ( CorrectTileOrPart( TE ) )
						return securityCheck( TE, player );
				}
			}
		}
		return false;
	}

	private boolean securityCheck(Object te, EntityPlayer player)
	{
		if ( te instanceof IActionHost && requiredPermission != null )
		{
			boolean requirePower = false;

			IGridNode gn = ((IActionHost) te).getActionableNode();
			if ( gn != null )
			{
				IGrid g = gn.getGrid();
				if ( g != null )
				{
					if ( requirePower )
					{
						IEnergyGrid eg = g.getCache( IEnergyGrid.class );
						if ( !eg.isNetworkPowered() )
						{
							return false;
						}
					}

					ISecurityGrid sg = g.getCache( ISecurityGrid.class );
					if ( sg.hasPermission( player, requiredPermission ) )
						return true;
				}
			}

			return false;
		}
		return true;
	}

}
