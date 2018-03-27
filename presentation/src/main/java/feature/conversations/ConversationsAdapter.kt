/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package feature.conversations

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.longClicks
import com.moez.QKSMS.R
import common.base.QkViewHolder
import common.util.Colors
import common.util.DateFormatter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import io.realm.RealmRecyclerViewAdapter
import kotlinx.android.synthetic.main.conversation_list_item.view.*
import model.Contact
import model.Message
import model.PhoneNumber
import javax.inject.Inject

class ConversationsAdapter @Inject constructor(
        private val context: Context,
        private val dateFormatter: DateFormatter,
        private val colors: Colors
) : RealmRecyclerViewAdapter<Message, QkViewHolder>(null, true) {

    val clicks: Subject<Long> = PublishSubject.create()
    val longClicks: Subject<Long> = PublishSubject.create()

    private val disposables = CompositeDisposable()
    private var attachedRecyclerViews = 0

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val layoutRes = when (viewType) {
            0 -> R.layout.conversation_list_item
            else -> R.layout.conversation_list_item_unread
        }

        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(layoutRes, parent, false)

        if (viewType == 1) {
            disposables += colors.theme
                    .subscribe { color -> view.date.setTextColor(color) }
        }

        return QkViewHolder(view)
    }

    fun isAttachedToRecyclerView() = attachedRecyclerViews > 0

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerViews++
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        attachedRecyclerViews--
        disposables.clear()
    }

    override fun onBindViewHolder(viewHolder: QkViewHolder, position: Int) {
        val message = getItem(position)!!
        val conversation = message.conversation!!
        val view = viewHolder.itemView

        view.clicks().subscribe { clicks.onNext(conversation.id) }
        view.longClicks().subscribe { longClicks.onNext(conversation.id) }

        view.avatars.contacts = conversation.recipients.map { recipient ->
            recipient.contact ?: Contact().apply { numbers.add(PhoneNumber().apply { address = recipient.address }) }
        }
        view.title.text = conversation.getTitle()
        view.date.text = dateFormatter.getConversationTimestamp(message.date)
        view.snippet.text = if (message.isMe()) "You: ${message.getSummary()}" else message.getSummary()
    }

    override fun getItemId(index: Int): Long {
        return getItem(index)!!.conversation!!.id
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)!!.read) 0 else 1
    }
}