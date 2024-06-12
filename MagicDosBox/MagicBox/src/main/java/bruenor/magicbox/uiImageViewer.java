/*
 *  Copyright (C) 2013-2016 Antony Hornacek (magicbox@imejl.sk)
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package bruenor.magicbox;

import magiclib.controls.ImageViewer;
import magiclib.controls.ImageViewerItem;
import magiclib.layout.widgets.Widget;
import magiclib.layout.widgets.WidgetType;

import android.content.Context;
import android.graphics.Color;

class uiImageViewer extends ImageViewer
{
	public uiImageViewer() {super();}

	public uiImageViewer(Context context) {
		super(context);
	}

	public uiImageViewer(Context context, boolean multi) {
		super(context, multi);
	}

	@Override
	public void addBackgrounds()
	{
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_01, "img_wbgr_01", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_02, "img_wbgr_02", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_03, "img_wbgr_03", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_04, "img_wbgr_04", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_05, "img_wbgr_05", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_06, "img_wbgr_06", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_07, "img_wbgr_07", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_08, "img_wbgr_08", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_09, "img_wbgr_09", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_10, "img_wbgr_10", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_11, "img_wbgr_11", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_12, "img_wbgr_12", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_13, "img_wbgr_13", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_14, "img_wbgr_14", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_15, "img_wbgr_15", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_19, "img_wbgr_19", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_16, "img_wbgr_16", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_17, "img_wbgr_17", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_18, "img_wbgr_18", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_20, "img_wbgr_20", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_21, "img_wbgr_21", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_22, "img_wbgr_22", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_23, "img_wbgr_23", "System", Color.YELLOW, false));
		systemBackgrounds.add(new ImageViewerItem(R.drawable.img_wbgr_24, "img_wbgr_24", "System", Color.YELLOW, false));
	}

	@Override
	public void addCrosshairs()
	{
		systemCrosshairs.add(new ImageViewerItem(R.drawable.icon_move_grey, "icon_move_grey", "System", Color.YELLOW, false));
		systemCrosshairs.add(new ImageViewerItem(R.drawable.img_cross_01, "img_cross_01", "System", Color.YELLOW, false));
		systemCrosshairs.add(new ImageViewerItem(R.drawable.img_cross_02, "img_cross_02", "System", Color.YELLOW, false));
		systemCrosshairs.add(new ImageViewerItem(R.drawable.img_cross_03, "img_cross_03", "System", Color.YELLOW, false));
		systemCrosshairs.add(new ImageViewerItem(R.drawable.img_cross_04, "img_cross_04", "System", Color.YELLOW, false));
	}

	@Override
	public void addImages()
	{
		systemImages.add(new ImageViewerItem(R.drawable.img_arrow_up, "img_arrow_up", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_arrow_down, "img_arrow_down", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_arrow_left, "img_arrow_left", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_arrow_right, "img_arrow_right", "System", Color.YELLOW, false));

		systemImages.add(new ImageViewerItem(R.drawable.img_hammer, "img_hammer", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_axe, "img_axe", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_blade, "img_blade", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_spikedclub, "img_spikedclub", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_flail, "img_flail", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_dagger, "img_dagger", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_bow, "img_bow", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_gnarledstaff, "img_gnarledstaff", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_flamesword, "img_flamesword", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_unit_attack, "img_unit_attack", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_magicanvil, "img_magicanvil", "System", Color.YELLOW, false));

		systemImages.add(new ImageViewerItem(R.drawable.img_magic, "img_magic", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_magicleaf, "img_magicleaf", "System", Color.YELLOW, false));

		systemImages.add(new ImageViewerItem(R.drawable.img_torch, "img_torch", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_fireball, "img_fireball", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_flamearrow, "img_flamearrow", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_firebolt, "img_firebolt", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_campfire, "img_campfire", "System", Color.YELLOW, false));

		systemImages.add(new ImageViewerItem(R.drawable.img_lightning, "img_lightning", "System", Color.YELLOW, false));

		systemImages.add(new ImageViewerItem(R.drawable.img_bag, "img_bag", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_pouch, "img_pouch", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_boots_of_speed, "img_boots_of_speed", "System", Color.YELLOW, false));

		systemImages.add(new ImageViewerItem(R.drawable.img_potionofpower, "img_potionofpower", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_potionblue, "img_potionblue", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_potionred, "img_potionred", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_potiongreen, "img_potiongreen", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_antidote, "img_antidote", "System", Color.YELLOW, false));

		systemImages.add(new ImageViewerItem(R.drawable.img_map, "img_map", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_journal, "img_journal", "System", Color.YELLOW, false));

		systemImages.add(new ImageViewerItem(R.drawable.img_flash, "img_flash", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_towngate, "img_towngate", "System", Color.YELLOW, false));


		systemImages.add(new ImageViewerItem(R.drawable.img_leftmouse, "img_leftmouse", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_rightmouse, "img_rightmouse", "System", Color.YELLOW, false));

		systemImages.add(new ImageViewerItem(R.drawable.img_button_a, "img_button_a", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_button_b, "img_button_b", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_button_x, "img_button_x", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_button_y, "img_button_y", "System", Color.YELLOW, false));

		systemImages.add(new ImageViewerItem(R.drawable.img_key, "img_key", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_scratch, "img_scratch", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_dummytarget, "img_dummytarget", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_masterofpuppets, "img_masterofpuppets", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_mousepaw, "img_mousepaw", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_navigation, "img_navigation", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_padlock, "img_padlock", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_nomouse, "img_nomouse", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_confirm, "img_confirm", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_footprint, "img_footprint", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_empty, "img_empty", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_ghost, "img_ghost", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_destroyundead, "img_destroyundead", "System", Color.YELLOW, false));
		systemImages.add(new ImageViewerItem(R.drawable.img_compass, "img_compass", "System", Color.YELLOW, false));
	}

	public boolean loadFoldersList()
	{
        images.clear();
        AppGlobal.addAvailableMappings(images, new WidgetType[]{WidgetType.folder}, true);

        grid.setAdapter(new ImageViewerAdapter(getContext(), android.R.layout.simple_list_item_1, images));

        return (images.size() > 0);

	}

	public static ImageViewerItem getImageViewerItemFromWidget(Widget widget)
	{
		int bmpCount = 0;

		ImageViewerItem item = new ImageViewerItem();

		item.setDescription(widget.getText());

		//i check widget with bitmapEnabled instead isBitmapEnabled, because widget may no be initialized yet
		if ((widget.bitmapEnabled == null) && widget.getBitmap() != null)
		{
			if (widget.getBitmap().getResourceName().contains("user_"))
			{
				item.setResourceID(-1);
				item.setFromFile(true);
			}
			else
			{
				int id = widget.getBitmapID();

				//widget may be inside bag which was not opened yet - this means that widget is not not initialized,
				//so we check for id manually
				if (id == -1)
					id = AppGlobal.getImageID(widget.getBitmap().getResourceName());

				item.setResourceID(id);
				item.setFromFile(false);
			}

			item.setName(widget.getBitmap().getResourceName());

			bmpCount++;
		}
		else
		{
			item.setResourceID(R.drawable.img_empty);
			item.setName("");
			item.setFromFile(false);
		}

		if (widget.bgrBitmapEnabled != null)
		{
			String name = widget.getBackgroundBitmap() == null? AppGlobal.defaultBackgroundImage:widget.getBackgroundBitmap();

			if (name.contains("user_"))
			{
				item.setBackgroundResourceID(-1);
				item.setBackgroundFromFile(true);
			}
			else
			{
				item.setBackgroundResourceID(AppGlobal.getImageID(name));
				item.setBackgroundFromFile(false);
			}

			item.setBackgroundName(name);

			bmpCount++;
		}
		else
		{
			item.setBackgroundResourceID(R.drawable.img_empty);
			item.setBackgroundName("");
			item.setBackgroundFromFile(false);
		}

		if (bmpCount == 0)
		{
			item.setResourceID(AppGlobal.empty_image);
			//item.setBackgroundResourceID(R.drawable.img_empty);
		}

		item.setTag(widget);

		return item;
	}
}